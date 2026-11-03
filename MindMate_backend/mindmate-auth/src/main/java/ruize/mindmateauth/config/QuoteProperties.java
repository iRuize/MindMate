package ruize.mindmateauth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 语录媒体本地存储配置，与 /media/quotes/** 静态映射共用同一目录。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Data
@Component
@ConfigurationProperties(prefix = "mindmate.quote")
public class QuoteProperties {
    private String directory = "./uploads/quotes";

    public Path storagePath() {
        return Paths.get(directory).toAbsolutePath().normalize();
    }
}
