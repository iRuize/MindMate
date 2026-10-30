package ruize.mindmateauth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地头像存储配置，与公开静态资源映射共用同一目录。
 *
 * @author 邸睿泽
 * @since 2026-10-30
 */
@Data
@Component
@ConfigurationProperties(prefix = "mindmate.avatar")
public class AvatarProperties {
    private String directory = "./uploads/avatars";

    public Path storagePath() {
        return Paths.get(directory).toAbsolutePath().normalize();
    }
}
