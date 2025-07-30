package ruize.mindmateauth.enums;

import lombok.Getter;

@Getter
public enum ResultCode {
    // 登录功能-短信验证码
    SEND_CODE_SUCCESS(201, "短信验证码发送成功"),
    PHONE_NUMBER_ILLEGAL(202, "手机号不合法"),
    SMSCODE_EXPIRED(203, "验证码已过期，请重新获取"),
    SMS_LOGIN_SUCCESS(204, "验证码校验成功，登录成功"),
    SMS_LOGIN_FAILED(205, "验证码错误，登录失败"),
    SMSCODE_TOO_MANY(206, "验证码发送次数过多，请稍后再试"),
    SMSCODE_SEND_TOO_FAST(207, "请稍后再试，不要频繁获取验证码");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
