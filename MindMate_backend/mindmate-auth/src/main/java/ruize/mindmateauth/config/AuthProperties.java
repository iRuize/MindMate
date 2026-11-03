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

    /**
     * 登录凭证有效期（小时），默认 7 天
     */
    private long tokenTtlHours = 168;

    /**
     * 是否在响应体中回传验证码。
     * 当前为伪装短信实现，置 true 便于自测；接入真实服务商后必须置 false。
     */
    private boolean exposeSmsCode = true;

    /**
     * 应急固定验证码。仅用于尚未接入真实短信的单机自托管场景，
     * 配置后可用该值登录任意已存在账号；接入短信服务商后必须置空。
     */
    private String fixedCode;
}
