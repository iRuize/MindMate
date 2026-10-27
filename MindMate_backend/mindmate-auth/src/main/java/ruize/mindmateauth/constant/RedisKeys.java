package ruize.mindmateauth.constant;

/**
 * Redis 键统一收口。
 * 原先冷却锁与验证码共用裸手机号，两者 TTL 被强行绑在一起，
 * 改任意一个都会误伤另一个，此处拆开并加业务前缀。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 短信验证码，值为明文验证码 */
    public static String smsCode(String phoneNumber) {
        return "sms:code:" + phoneNumber;
    }

    /** 发送冷却标记，仅用于判断是否处于冷却窗口，不存业务数据 */
    public static String smsCooldown(String phoneNumber) {
        return "sms:cooldown:" + phoneNumber;
    }

    /** 当日发送计数，过期时间为次日零点 */
    public static String smsDaily(String phoneNumber) {
        return "sms:daily:" + phoneNumber;
    }

    /** 验证码校验失败次数，与验证码同生命周期 */
    public static String smsAttempt(String phoneNumber) {
        return "sms:attempt:" + phoneNumber;
    }

    /** 登录凭证，值为用户ID */
    public static String authToken(String token) {
        return "auth:token:" + token;
    }
}
