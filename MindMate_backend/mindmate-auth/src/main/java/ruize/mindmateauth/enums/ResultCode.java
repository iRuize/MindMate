package ruize.mindmateauth.enums;

import lombok.Data;

public enum ResultCode {
    //登陆成功
    SUCCESS(200, "登录成功"),
//    短信验证码
    SEND_CODE_SUCCESS(201, "短信验证码发送成功"),
    SEND_CODE_FAILED(202,"验证码发送失败"),
    UNAUTHORIZED(401, "未授权"),
    ERROR(500, "服务器错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }
}
