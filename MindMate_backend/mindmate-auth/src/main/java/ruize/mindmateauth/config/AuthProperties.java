package ruize.mindmateauth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证相关配置。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
@Data
@Component
@ConfigurationProperties(prefix = "mindmate.auth")
public class AuthProperties {

    /** 登录凭证有效期（小时），默认 7 天 */
    private long tokenTtlHours = 168;

    /**
     * 是否在响应体中回传验证码。
     * 当前为伪装短信实现，置 true 便于自测；接入真实服务商后必须置 false。
     */
    private boolean exposeSmsCode = true;
}
