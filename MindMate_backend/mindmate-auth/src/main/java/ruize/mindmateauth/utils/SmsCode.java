package ruize.mindmateauth.utils;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * 短信验证码工具。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
public final class SmsCode {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private SmsCode() {
    }

    /**
     * 生成 6 位数字验证码。
     * 必须返回字符串：以 0 开头的验证码若转成 int 会丢掉前导零，退化为 5 位。
     *
     * @return 定长 6 位的数字串
     */
    public static String generateSmsCode() {
        StringBuilder builder = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            builder.append(ThreadLocalRandom.current().nextInt(10));
        }
        return builder.toString();
    }

    /**
     * 校验中国大陆手机号。
     *
     * @param phoneNumber 手机号
     * @return true 表示合法
     */
    public static boolean validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return false;
        }
        return PHONE_PATTERN.matcher(phoneNumber.trim()).matches();
    }
}
