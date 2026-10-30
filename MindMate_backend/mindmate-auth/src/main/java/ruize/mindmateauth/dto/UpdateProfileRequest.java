package ruize.mindmateauth.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

/**
 * 当前用户资料。空值或空白字符串用于清空对应字段。
 *
 * @author 邸睿泽
 * @since 2026-10-30
 */
@Data
public class UpdateProfileRequest {
    @Size(max = 50, message = "用户名不能超过50个字符")
    private String username;

    @Size(max = 100, message = "邮箱不能超过100个字符")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 255, message = "个性签名不能超过255个字符")
    private String personalizedSignature;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
