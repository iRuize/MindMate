package ruize.mindmateauth.dto;

import lombok.Data;

/**
 * 登录请求体。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
@Data
public class LoginRequest {
    private String phoneNumber;
    private String smsCode;
}
