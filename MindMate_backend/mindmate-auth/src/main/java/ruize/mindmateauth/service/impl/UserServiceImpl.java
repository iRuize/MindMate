package ruize.mindmateauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ruize.mindmateauth.aop.SmsSendRecord;
import ruize.mindmateauth.config.AuthProperties;
import ruize.mindmateauth.constant.RedisKeys;
import ruize.mindmateauth.dto.LoginResponse;
import ruize.mindmateauth.dto.UpdateProfileRequest;
import ruize.mindmateauth.dto.UserVO;
import ruize.mindmateauth.entity.User;
import ruize.mindmateauth.enums.ResultCode;
import ruize.mindmateauth.exception.BizException;
import ruize.mindmateauth.mapper.UserMapper;
import ruize.mindmateauth.service.AvatarStorage;
import ruize.mindmateauth.service.UserService;
import ruize.mindmateauth.utils.SmsCode;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户与登录服务实现。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    /** 验证码有效期 */
    private static final Duration CODE_TTL = Duration.ofMinutes(2);
    /** 同一手机号重复发送的冷却窗口 */
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);
    /** 单日发送上限 */
    private static final int DAILY_LIMIT = 5;
    /** 单个验证码允许的最大校验失败次数 */
    private static final int MAX_ATTEMPT = 5;

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final AuthProperties authProperties;
    private final AvatarStorage avatarStorage;

    public UserServiceImpl(UserMapper userMapper, StringRedisTemplate stringRedisTemplate,
                           AuthProperties authProperties, AvatarStorage avatarStorage) {
        this.userMapper = userMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.authProperties = authProperties;
        this.avatarStorage = avatarStorage;
    }

    @Override
    @SmsSendRecord
    public String sendSmsCode(String phoneNumber) {
        if (!SmsCode.validatePhoneNumber(phoneNumber)) {
            throw new BizException(ResultCode.PHONE_NUMBER_ILLEGAL);
        }
        String phone = phoneNumber.trim();

        // 冷却窗口用 setIfAbsent 原子占位。原实现先 hasKey 再 set 存在竞态，
        // 且与验证码共用同一 key，导致冷却时长被验证码 TTL 绑死。
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisKeys.smsCooldown(phone), "1", SEND_COOLDOWN);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BizException(ResultCode.SMSCODE_SEND_TOO_FAST);
        }

        // 当日限额
        String dailyKey = RedisKeys.smsDaily(phone);
        Long count = stringRedisTemplate.opsForValue().increment(dailyKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(dailyKey, secondsUntilMidnight());
        }
        if (count != null && count > DAILY_LIMIT) {
            throw new BizException(ResultCode.SMSCODE_TOO_MANY);
        }

        // 首次登录即完成注册
        if (findByPhone(phone) == null) {
            userMapper.insert(new User().setPhoneNumber(phone));
            log.info("手机号 {} 首次登录，已自动创建账号", phone);
        }

        String code = SmsCode.generateSmsCode();
        stringRedisTemplate.opsForValue().set(RedisKeys.smsCode(phone), code, CODE_TTL);
        stringRedisTemplate.delete(RedisKeys.smsAttempt(phone));

        // 伪装短信：不对接服务商，验证码仅落日志并按配置回传，便于自测
        log.info("[MOCK-SMS] {} 的验证码为 {}，有效期 2 分钟", phone, code);
        return authProperties.isExposeSmsCode() ? code : null;
    }

    @Override
    public LoginResponse login(String phoneNumber, String smsCode) {
        if (!StringUtils.hasText(phoneNumber) || !StringUtils.hasText(smsCode)) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }
        String phone = phoneNumber.trim();
        if (!SmsCode.validatePhoneNumber(phone)) {
            throw new BizException(ResultCode.PHONE_NUMBER_ILLEGAL);
        }

        User user = findByPhone(phone);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }

        // 先判锁定，避免已被锁定的手机号继续消耗尝试次数
        if (currentAttempt(phone) >= MAX_ATTEMPT) {
            throw new BizException(ResultCode.SMSCODE_ATTEMPT_LIMIT);
        }

        String cachedCode = stringRedisTemplate.opsForValue().get(RedisKeys.smsCode(phone));
        if (cachedCode == null) {
            throw new BizException(ResultCode.SMSCODE_EXPIRED);
        }

        if (!cachedCode.equals(smsCode.trim())) {
            int failed = increaseAttempt(phone);
            log.warn("手机号 {} 验证码错误，累计 {} 次", phone, failed);
            throw new BizException(failed >= MAX_ATTEMPT
                    ? ResultCode.SMSCODE_ATTEMPT_LIMIT
                    : ResultCode.SMSCODE_WRONG);
        }

        // 校验通过：验证码一次性消费，同时清空尝试计数
        stringRedisTemplate.delete(RedisKeys.smsCode(phone));
        stringRedisTemplate.delete(RedisKeys.smsAttempt(phone));

        LocalDateTime now = LocalDateTime.now();
        userMapper.updateById(new User()
                .setUserId(user.getUserId())
                .setLastLoginAt(now)
                .setUpdatedAt(now));
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);

        String token = issueToken(user.getUserId());
        log.info("用户 {} 登录成功", user.getUserId());
        return new LoginResponse(token, UserVO.from(user));
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return UserVO.from(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        getCurrentUser(userId);
        // 显式 set 才能将空值写入数据库，不受 MyBatis-Plus 默认忽略 null 的策略影响。
        int updated = userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getUserId, userId)
                .set(User::getUsername, normalizeProfileValue(request.getUsername()))
                .set(User::getEmail, normalizeProfileValue(request.getEmail()))
                .set(User::getPersonalizedSignature, normalizeProfileValue(request.getPersonalizedSignature()))
                .set(User::getUpdatedAt, LocalDateTime.now()));
        if (updated != 1) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return getCurrentUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO uploadAvatar(Long userId, MultipartFile file) {
        getCurrentUser(userId);
        Path newFile = avatarStorage.store(file);
        try {
            // 在事务真正完成后决定是否清理，覆盖方法返回后提交失败的情况。
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        avatarStorage.deleteNewFile(newFile);
                    }
                }
            });
            int updated = userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getUserId, userId)
                    .set(User::getAvatarUrl, "/avatars/" + newFile.getFileName())
                    .set(User::getUpdatedAt, LocalDateTime.now()));
            if (updated != 1) {
                throw new BizException(ResultCode.USER_NOT_FOUND);
            }
            return getCurrentUser(userId);
        } catch (RuntimeException | Error e) {
            avatarStorage.deleteNewFile(newFile);
            throw e;
        }
    }

    private String normalizeProfileValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    @Override
    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            stringRedisTemplate.delete(RedisKeys.authToken(token));
        }
    }

    private User findByPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhoneNumber, phone));
    }

    private String issueToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(RedisKeys.authToken(token), String.valueOf(userId),
                Duration.ofHours(authProperties.getTokenTtlHours()));
        return token;
    }

    private int currentAttempt(String phone) {
        String value = stringRedisTemplate.opsForValue().get(RedisKeys.smsAttempt(phone));
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return MAX_ATTEMPT;
        }
    }

    private int increaseAttempt(String phone) {
        String key = RedisKeys.smsAttempt(phone);
        Long failed = stringRedisTemplate.opsForValue().increment(key);
        if (failed != null && failed == 1L) {
            stringRedisTemplate.expire(key, CODE_TTL);
        }
        return failed == null ? MAX_ATTEMPT : failed.intValue();
    }

    /**
     * 距次日零点的剩余时长。
     * 原实现取「当天 23:59:59」作差值，在 23:59:59 之后会得到负值，
     * expire 传负数会让 key 立即失效，导致当日计数被重置。
     */
    private Duration secondsUntilMidnight() {
        long seconds = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay()).getSeconds();
        return Duration.ofSeconds(Math.max(seconds, 1));
    }
}
