package ruize.mindmateauth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应体。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    /** 登录凭证，后续请求置于 Authorization: Bearer {token} */
    private String token;
    private UserVO user;
}
