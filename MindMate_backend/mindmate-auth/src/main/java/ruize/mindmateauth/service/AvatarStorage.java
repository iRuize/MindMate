package ruize.mindmateauth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ruize.mindmateauth.config.AvatarProperties;
import ruize.mindmateauth.enums.ResultCode;
import ruize.mindmateauth.exception.BizException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.UUID;

/**
 * 头像解码与本地存储。只保存重新编码的 PNG，不使用客户端文件名和 MIME 类型。
 *
 * @author 邸睿泽
 * @since 2026-10-30
 */
@Slf4j
@Component
public class AvatarStorage {
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;
    private static final long MAX_PIXELS = 16_000_000L;

    private final AvatarProperties avatarProperties;

    public AvatarStorage(AvatarProperties avatarProperties) {
        this.avatarProperties = avatarProperties;
    }

    public Path store(MultipartFile file) {
        BufferedImage image = decode(file);
        Path directory = avatarProperties.storagePath();
        Path target = directory.resolve(UUID.randomUUID() + ".png").normalize();
        if (!target.getParent().equals(directory)) {
            throw new BizException(ResultCode.PARAM_INVALID, "头像文件路径不合法");
        }

        boolean created = false;
        boolean stored = false;
        try {
            Files.createDirectories(directory);
            try (OutputStream output = Files.newOutputStream(target,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                created = true;
                if (!ImageIO.write(image, "png", output)) {
                    throw new IOException("PNG 编码器不可用");
                }
            }
            stored = true;
            return target;
        } catch (IOException e) {
            log.error("头像文件保存失败", e);
            throw new BizException(ResultCode.SERVER_ERROR, "头像保存失败，请稍后重试");
        } finally {
            image.flush();
            if (created && !stored) {
                deleteNewFile(target);
            }
        }
    }

    /** 仅清理本次请求创建的文件，旧头像保留以避免并发更新误删。 */
    public void deleteNewFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException | SecurityException e) {
            log.error("清理本次上传的头像失败: {}", path, e);
        }
    }

    private BufferedImage decode(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_INVALID, "请选择头像文件");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BizException(ResultCode.PARAM_INVALID, "头像文件不能超过2MB");
        }

        try (InputStream input = file.getInputStream()) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) {
                throw new BizException(ResultCode.PARAM_INVALID, "头像文件不能超过2MB");
            }
            try (ImageInputStream imageInput = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
                if (!readers.hasNext()) {
                    throw new BizException(ResultCode.PARAM_INVALID, "头像必须是有效的PNG或JPG图片");
                }
                ImageReader reader = readers.next();
                try {
                    String format = reader.getFormatName();
                    if (!"png".equalsIgnoreCase(format) && !"jpeg".equalsIgnoreCase(format)) {
                        throw new BizException(ResultCode.PARAM_INVALID, "头像仅支持PNG或JPG图片");
                    }
                    // 解码器对截断等损坏可能只发出警告，统一按无效图片拒绝。
                    reader.addIIOReadWarningListener((source, warning) -> {
                        throw new IllegalArgumentException("头像图片解码警告");
                    });
                    reader.setInput(imageInput, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    // 在解码前限制像素，避免小体积压缩图片耗尽内存。
                    if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION
                            || (long) width * height > MAX_PIXELS) {
                        throw new BizException(ResultCode.PARAM_INVALID, "头像宽高不能超过4096像素，总像素不能超过1600万");
                    }
                    BufferedImage decoded = reader.read(0);
                    if (decoded == null) {
                        throw new BizException(ResultCode.PARAM_INVALID, "头像图片无法解码，请重新选择");
                    }
                    BufferedImage normalized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = normalized.createGraphics();
                    try {
                        graphics.drawImage(decoded, 0, 0, null);
                    } finally {
                        graphics.dispose();
                        decoded.flush();
                    }
                    return normalized;
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new BizException(ResultCode.PARAM_INVALID, "头像图片无效或已损坏，请重新选择PNG或JPG图片");
        }
    }
}
