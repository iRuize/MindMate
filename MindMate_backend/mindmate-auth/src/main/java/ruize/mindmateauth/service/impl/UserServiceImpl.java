package ruize.mindmateauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ruize.mindmateauth.aop.SmsSendRecord;
import ruize.mindmateauth.entity.User;
import ruize.mindmateauth.enums.ResultCode;
import ruize.mindmateauth.mapper.UserMapper;
import ruize.mindmateauth.result.Result;
import ruize.mindmateauth.service.UserService;
import ruize.mindmateauth.utils.SmsCode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public UserServiceImpl(UserMapper userMapper, StringRedisTemplate stringRedisTemplate) {
        this.userMapper = userMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 发送短信验证码
    @Override
    // 自定义注解，实现AOP切面
    @SmsSendRecord()
    public Result<Integer> sendSmsCode(String phoneNumber) {
        //校验手机号是否合理
        boolean validateResult = SmsCode.validatePhoneNumber(phoneNumber);
        if (!validateResult) {
            return Result.getResult(ResultCode.PHONE_NUMBER_ILLEGAL.getCode(),
                    ResultCode.PHONE_NUMBER_ILLEGAL.getMessage(), null);
        }
        //限制最短发送时间间隔
        Boolean hasKey = stringRedisTemplate.hasKey(phoneNumber);
        if (hasKey) {
            return Result.getResult(ResultCode.SMSCODE_SEND_TOO_FAST.getCode(),
                    ResultCode.SMSCODE_SEND_TOO_FAST.getMessage(), null);
        }
        //检验当前用户当日发送次数是否超过5次
        String countKey = "sms:count:" + phoneNumber;
        Long count = stringRedisTemplate.opsForValue().increment(countKey);
        if (count != null && count == 1) {
            // 设置过期时间为当天24点
            long secondsUntilEndOfDay = Duration.between(LocalDateTime.now(), LocalDateTime.now().toLocalDate().atTime(23, 59, 59)).getSeconds();
            stringRedisTemplate.expire(countKey, secondsUntilEndOfDay, TimeUnit.SECONDS);
        }
        if (count != null && count > 5) {
            return Result.getResult(ResultCode.SMSCODE_TOO_MANY.getCode(),
                    ResultCode.SMSCODE_TOO_MANY.getMessage(), null);
        }
        //校验用户是否存在
        QueryWrapper<User> getUserByPhoneNumber = new QueryWrapper<User>().eq("phone_number", phoneNumber);
        boolean exists = userMapper.exists(getUserByPhoneNumber);
        int smsCode = SmsCode.generateSmsCode();
        // 用户存在话直接发送短信验证码存入Redis即可
        if (exists) {
            // 保存短信验证码到数据库，有效期2分钟
            stringRedisTemplate.opsForValue().set(phoneNumber, String.valueOf(smsCode),
                    2, TimeUnit.MINUTES);
        } else {
            // 用户不存在的话需要将用户信息写入Mysql数据库，然后再发送短信验证码
            User newUser = new User().setPhoneNumber(phoneNumber);
            // 自动注册逻辑
            userMapper.insert(newUser);
            stringRedisTemplate.opsForValue().set(phoneNumber, String.valueOf(smsCode), 2, TimeUnit.MINUTES);
        }
        return Result.getResult(ResultCode.SEND_CODE_SUCCESS.getCode(),
                ResultCode.SEND_CODE_SUCCESS.getMessage(), smsCode);
    }


    // 短信验证码登录
    @Override
    public Result<Integer> smsLogin(String phoneNumber, String smsCodeInput) {
        //登陆之前检测当前用户是否在数据库中存在——防止恶意攻击
        QueryWrapper<User> queryUser = new QueryWrapper<User>().eq("phone_number", phoneNumber);
        User user = userMapper.selectOne(queryUser);
        if (user == null) {
            return Result.getResult(100, "请获取验证码后再尝试登录", null);
        }
        // 从Redis中获取当前用户正确的短信验证码
        String realSmsCode = stringRedisTemplate.opsForValue().get(phoneNumber);

        // 验证码已过期
        if (realSmsCode == null) {
            return Result.getResult(ResultCode.SMSCODE_EXPIRED.getCode(),
                    ResultCode.SMSCODE_EXPIRED.getMessage(), null);
        }

        // 验证码正确
        if (realSmsCode.equals(smsCodeInput)) {
            //删除Redis中缓存的验证码
            stringRedisTemplate.delete(phoneNumber);
            return Result.getResult(ResultCode.SMS_LOGIN_SUCCESS.getCode(),
                    ResultCode.SMS_LOGIN_SUCCESS.getMessage(), null);
        }

        // 验证码错误
        return Result.getResult(ResultCode.SMS_LOGIN_FAILED.getCode(),
                ResultCode.SMS_LOGIN_FAILED.getMessage(), null);
    }
}
