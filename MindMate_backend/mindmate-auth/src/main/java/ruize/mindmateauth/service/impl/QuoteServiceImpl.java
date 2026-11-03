package ruize.mindmateauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ruize.mindmateauth.context.UserContext;
import ruize.mindmateauth.dto.QuoteVO;
import ruize.mindmateauth.entity.Quote;
import ruize.mindmateauth.enums.ResultCode;
import ruize.mindmateauth.exception.BizException;
import ruize.mindmateauth.mapper.QuoteMapper;
import ruize.mindmateauth.service.QuoteService;
import ruize.mindmateauth.service.QuoteStorage;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 语录内容入库与媒体的先落盘、后写库。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Service
public class QuoteServiceImpl implements QuoteService {

    private static final int CONTENT_MAX_LENGTH = 2000;
    private static final int SOURCE_MAX_LENGTH = 200;
    private static final String MEDIA_URL_PREFIX = "/media/quotes/";

    private final QuoteMapper quoteMapper;
    private final QuoteStorage quoteStorage;

    public QuoteServiceImpl(QuoteMapper quoteMapper, QuoteStorage quoteStorage) {
        this.quoteMapper = quoteMapper;
        this.quoteStorage = quoteStorage;
    }

    @Override
    public List<QuoteVO> listQuotes() {
        Long userId = currentUserId();
        List<Quote> quotes = quoteMapper.selectList(new LambdaQueryWrapper<Quote>()
                .eq(Quote::getUserId, userId)
                .orderByDesc(Quote::getId));
        return quotes.stream().map(QuoteVO::from).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuoteVO createQuote(String content, String source, MultipartFile file) {
        Long userId = currentUserId();
        String normalizedContent = normalize(content, CONTENT_MAX_LENGTH, "语录内容不能超过2000个字符");
        String normalizedSource = normalize(source, SOURCE_MAX_LENGTH, "语录出处不能超过200个字符");
        boolean hasFile = file != null && !file.isEmpty();
        if (normalizedContent == null && !hasFile) {
            throw new BizException(ResultCode.PARAM_INVALID, "语录内容与媒体文件不能同时为空");
        }

        QuoteStorage.StoredMedia media = hasFile ? quoteStorage.store(file) : null;
        Path mediaPath = media == null ? null : media.path();
        if (mediaPath != null) {
            registerRollbackCleanup(mediaPath);
        }

        Quote quote = new Quote()
                .setUserId(userId)
                .setContent(normalizedContent)
                .setSource(normalizedSource)
                .setMediaType(media == null ? 0 : media.mediaType())
                .setMediaUrl(mediaPath == null ? null : MEDIA_URL_PREFIX + mediaPath.getFileName())
                .setCreatedAt(LocalDateTime.now())
                .setDeleted(0);
        try {
            if (quoteMapper.insert(quote) != 1) {
                throw new BizException(ResultCode.QUOTE_WRITE_FAILED);
            }
            return QuoteVO.from(quote);
        } catch (RuntimeException | Error e) {
            // 写库失败时立即清理刚落盘的文件，避免留下无引用媒体
            if (mediaPath != null) {
                quoteStorage.deleteNewFile(mediaPath);
            }
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuote(Long id) {
        Long userId = currentUserId();
        if (id == null) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }
        // 只做逻辑删除，媒体文件保留：并发读取可能仍在使用同一文件，
        // 且删除后无法判断文件是否还有其他引用，残留文件交由运维按目录定期清理。
        int affected = quoteMapper.update(null, new LambdaUpdateWrapper<Quote>()
                .eq(Quote::getId, id)
                .eq(Quote::getUserId, userId)
                .set(Quote::getDeleted, 1));
        if (affected != 1) {
            throw new BizException(ResultCode.QUOTE_NOT_FOUND);
        }
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    private String normalize(String value, int maxLength, String message) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BizException(ResultCode.PARAM_INVALID, message);
        }
        return trimmed;
    }

    /**
     * 事务未提交时清理本次写入的文件，覆盖方法返回后提交失败的情况。
     */
    private void registerRollbackCleanup(Path mediaPath) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    quoteStorage.deleteNewFile(mediaPath);
                }
            }
        });
    }
}
