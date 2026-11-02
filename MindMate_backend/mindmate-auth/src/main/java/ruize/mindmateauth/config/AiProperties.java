package ruize.mindmateauth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * DeepSeek 调用配置。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mindmate.ai")
public class AiProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-flash";

    public boolean isReady() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(baseUrl) && StringUtils.hasText(model);
    }
}
