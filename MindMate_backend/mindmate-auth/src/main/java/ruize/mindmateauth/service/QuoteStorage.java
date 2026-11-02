package ruize.mindmateauth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ruize.mindmateauth.config.QuoteProperties;
import ruize.mindmateauth.enums.ResultCode;
import ruize.mindmateauth.exception.BizException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 语录媒体的类型识别与本地存储。
 * 图片按文件魔数识别，视频按 content-type 与扩展名双重白名单校验；
 * 落盘文件名一律用 UUID 重新生成，不沿用客户端文件名。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Slf4j
@Component
public class QuoteStorage {
    /**
     * 媒体类型：1图片 2视频，与 QUOTE.MEDIA_TYPE 的 CHECK 约束一致
     */
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 50L * 1024 * 1024;
    private static final int HEAD_BYTES = 12;
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm");

    private final QuoteProperties quoteProperties;

    public QuoteStorage(QuoteProperties quoteProperties) {
        this.quoteProperties = quoteProperties;
    }

    /**
     * 校验并保存媒体文件，返回媒体类型与实际落盘文件名。
     */
    public StoredMedia store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_INVALID, "请选择要上传的图片或视频");
        }

        String imageExtension = detectImageExtension(readHead(file));
        if (imageExtension != null) {
            if (file.getSize() > MAX_IMAGE_BYTES) {
                throw new BizException(ResultCode.QUOTE_MEDIA_TOO_LARGE, "图片不能超过5MB");
            }
            Path stored = writeFile(file, imageExtension);
            return new StoredMedia(MEDIA_TYPE_IMAGE, stored);
        }

        String videoExtension = resolveVideoExtension(file);
        if (file.getSize() > MAX_VIDEO_BYTES) {
            throw new BizException(ResultCode.QUOTE_MEDIA_TOO_LARGE, "视频不能超过50MB");
        }
        Path stored = writeFile(file, videoExtension);
        return new StoredMedia(MEDIA_TYPE_VIDEO, stored);
    }

    /**
     * 仅清理本次请求写入的文件，已入库的媒体不在此处理。
     */
    public void deleteNewFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException | SecurityException e) {
            log.error("清理本次上传的语录媒体失败: {}", path, e);
        }
    }

    private byte[] readHead(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            return input.readNBytes(HEAD_BYTES);
        } catch (IOException e) {
            throw new BizException(ResultCode.PARAM_INVALID, "文件读取失败，请重新上传");
        }
    }

    private String detectImageExtension(byte[] head) {
        if (head.length >= 8 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G'
                && (head[4] & 0xFF) == 0x0D && (head[5] & 0xFF) == 0x0A
                && (head[6] & 0xFF) == 0x1A && (head[7] & 0xFF) == 0x0A) {
            return "png";
        }
        if (head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (head.length >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private String resolveVideoExtension(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String contentType = file.getContentType();
        if (extension == null || contentType == null) {
            throw new BizException(ResultCode.QUOTE_MEDIA_INVALID);
        }

        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        if (!VIDEO_EXTENSIONS.contains(normalizedExtension)) {
            throw new BizException(ResultCode.QUOTE_MEDIA_INVALID);
        }

        String expectedContentType = "mp4".equals(normalizedExtension) ? "video/mp4" : "video/webm";
        if (!expectedContentType.equalsIgnoreCase(contentType.trim())) {
            throw new BizException(ResultCode.QUOTE_MEDIA_INVALID);
        }
        return normalizedExtension;
    }

    private Path writeFile(MultipartFile file, String extension) {
        Path directory = quoteProperties.storagePath();
        Path target = directory.resolve(UUID.randomUUID() + "." + extension).normalize();
        if (!directory.equals(target.getParent())) {
            throw new BizException(ResultCode.PARAM_INVALID, "语录文件路径不合法");
        }

        boolean stored = false;
        try {
            Files.createDirectories(directory);
            // 目标文件不存在才会创建，UUID 重名时宁可失败也不覆盖已有媒体
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }
            stored = true;
            return target;
        } catch (IOException e) {
            log.error("语录媒体保存失败", e);
            throw new BizException(ResultCode.QUOTE_STORAGE_FAILED);
        } finally {
            if (!stored) {
                deleteNewFile(target);
            }
        }
    }

    /**
     * 落盘结果：mediaType 与 QUOTE.MEDIA_TYPE 取值一致，path 为实际写入的文件。
     */
    public record StoredMedia(int mediaType, Path path) {
    }
}
