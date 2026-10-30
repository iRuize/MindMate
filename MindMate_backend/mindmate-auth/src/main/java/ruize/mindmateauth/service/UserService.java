package ruize.mindmateauth.service;

import org.springframework.web.multipart.MultipartFile;
import ruize.mindmateauth.dto.LoginResponse;
import ruize.mindmateauth.dto.UpdateProfileRequest;
import ruize.mindmateauth.dto.UserVO;

/**
 * 用户与登录服务。
 * 失败情况一律抛 BizException，由全局异常处理器转为统一响应。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
public interface UserService {

    /**
     * 发送短信验证码（当前为伪装实现，不接短信服务商）。
     *
     * @return 验证码；当 expose-sms-code 关闭时返回 null
     */
    String sendSmsCode(String phoneNumber);

    /**
     * 验证码登录。校验通过后签发登录凭证并刷新最近登录时间。
     */
    LoginResponse login(String phoneNumber, String smsCode);

    /**
     * 查询当前登录用户信息。
     */
    UserVO getCurrentUser(Long userId);

    /**
     * 更新当前用户资料，空值或空白字符串清空对应字段。
     */
    UserVO updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 上传头像并更新当前用户的公开头像路径。
     */
    UserVO uploadAvatar(Long userId, MultipartFile file);

    /**
     * 退出登录，使凭证立即失效。
     */
    void logout(String token);
}
