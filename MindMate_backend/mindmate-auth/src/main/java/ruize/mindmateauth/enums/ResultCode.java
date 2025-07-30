package ruize.mindmateauth.enums;

import lombok.Getter;

@Getter
public enum ResultCode {
    // 登录功能-短信验证码
    SEND_CODE_SUCCESS(201, "短信验证码发送成功"),
    SEND_CODE_FAILED(202, "验证码发送失败，请先进行注册"),
    SMSCODE_EXPIRED(203, "验证码已过期，请重新获取"),
    SMS_LOGIN_SUCCESS(204, "验证码校验成功，登录成功"),
    SMS_LOGIN_FAILED(205, "验证码错误，登录失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
