package ruize.mindmateauth.enums;

import lombok.Getter;

/**
 * 业务返回码。
 * 200 为统一成功码，1001 起为业务错误，400/401 与 HTTP 语义对齐。
 * HTTP 状态码恒为 200，业务结果一律由响应体的 code 承载。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
@Getter
public enum ResultCode {
    SUCCESS(200, "成功"),

    PARAM_INVALID(400, "参数不合法"),
    UNAUTHORIZED(401, "登录状态已失效，请重新登录"),

    PHONE_NUMBER_ILLEGAL(1001, "手机号格式不正确"),
    SMSCODE_SEND_TOO_FAST(1002, "操作过于频繁，请稍后再试"),
    SMSCODE_TOO_MANY(1003, "今日验证码获取次数已达上限"),
    SMSCODE_EXPIRED(1004, "验证码已过期，请重新获取"),
    SMSCODE_WRONG(1005, "验证码错误"),
    SMSCODE_ATTEMPT_LIMIT(1006, "验证码错误次数过多，请重新获取"),
    USER_NOT_FOUND(1007, "用户不存在，请先获取验证码"),
    TASK_NOT_FOUND(2001, "任务不存在或已删除"),
    TASK_STATUS_INVALID(2002, "任务状态只能为0或2"),
    TASK_WRITE_FAILED(2003, "任务保存失败，请稍后再试"),
    SERVER_ERROR(1500, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
