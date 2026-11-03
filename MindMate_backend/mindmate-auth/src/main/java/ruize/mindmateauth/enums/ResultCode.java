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
    DECISION_PARAM_INVALID(3000, "决策请求参数不合法，请检查后重试"),
    DECISION_AI_NOT_CONFIGURED(3001, "决策模型尚未配置，请稍后再试"),
    DECISION_PREFERENCE_INVALID(3002, "偏好内容不合法，请检查后重试"),
    DECISION_SESSION_NOT_FOUND(3003, "决策会话不存在或已删除"),
    DECISION_MESSAGE_INVALID(3004, "消息内容不合法，请检查后重试"),
    DECISION_AI_TIMEOUT(3005, "决策模型响应超时，请稍后重试"),
    DECISION_AI_UNAUTHORIZED(3006, "决策模型配置无效，请联系管理员"),
    DECISION_AI_PAYMENT_REQUIRED(3007, "决策模型服务不可用，请联系管理员"),
    DECISION_AI_RATE_LIMITED(3008, "决策模型请求过于频繁，请稍后重试"),
    DECISION_AI_SERVICE_ERROR(3009, "决策模型暂时不可用，请稍后重试"),
    DECISION_LOCKED(3010, "该决策会话正在处理中，请稍后再试"),
    DECISION_WRITE_FAILED(3011, "决策保存失败，请稍后再试"),
    QUOTE_NOT_FOUND(4001, "语录不存在或已删除"),
    QUOTE_MEDIA_INVALID(4002, "媒体格式不支持，图片支持JPG、PNG、WEBP，视频支持MP4、WEBM"),
    QUOTE_MEDIA_TOO_LARGE(4003, "图片不能超过5MB，视频不能超过50MB"),
    QUOTE_WRITE_FAILED(4004, "语录保存失败，请稍后再试"),
    QUOTE_STORAGE_FAILED(4005, "语录媒体保存失败，请稍后再试"),
    SERVER_ERROR(1500, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
