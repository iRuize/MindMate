package ruize.mindmateauth.utils;

import java.util.Random;
import java.util.regex.Pattern;

public class SmsCode {
    /**
     * 生成6位数字验证码
     * @return 6位数字验证码
     */
    public static int generateSmsCode() {
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            stringBuilder.append(random.nextInt(10));
        }
        // 转换成为整数
        return Integer.parseInt(stringBuilder.toString());
    }

    /**
     * @param phoneNumber 手机号码
     * @return true 表示合法，false 表示不合法
     */
    public static boolean validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return false;
        }
        // 去除首尾空白字符
        phoneNumber = phoneNumber.trim();

        // 简单长度判断（中国手机号长度固定为11位）
        if (phoneNumber.length() != 11) {
            return false;
        }
        // 正则表达式：1开头，第二位数字可能是3-9中的任意一位，后面9位是数字
        String regex = "^1[3-9]\\d{9}$";
        return Pattern.matches(regex, phoneNumber);
    }
}
