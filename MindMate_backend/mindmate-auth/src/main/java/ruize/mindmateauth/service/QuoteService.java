package ruize.mindmateauth.service;

import org.springframework.web.multipart.MultipartFile;
import ruize.mindmateauth.dto.QuoteVO;

import java.util.List;

/**
 * 当前登录用户的经典语录服务。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
public interface QuoteService {
    List<QuoteVO> listQuotes();

    QuoteVO createQuote(String content, String source, MultipartFile file);

    void deleteQuote(Long id);
}
