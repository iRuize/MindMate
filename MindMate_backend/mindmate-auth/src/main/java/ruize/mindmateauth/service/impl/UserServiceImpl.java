package ruize.mindmateauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ruize.mindmateauth.entity.User;
import ruize.mindmateauth.enums.ResultCode;
import ruize.mindmateauth.mapper.UserMapper;
import ruize.mindmateauth.result.Result;
import ruize.mindmateauth.service.UserService;

import java.util.Random;
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
    public Result<Integer> sendSmsCode(String phoneNumber) {
        //校验用户是否存在
        QueryWrapper<User> getUserByPhoneNumber = new QueryWrapper<User>().eq("phone_number", phoneNumber);
        boolean exists = userMapper.exists(getUserByPhoneNumber);
        if (exists) {
            //发送短信验证码
            Random random = new Random();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                stringBuilder.append(random.nextInt(10));
            }
            // 保存短信验证码到数据库，有效期2分钟
            // 笔记本屏幕太小啦~，不知道何时可以用MacBook Pro搭5K显示器。2025-07-30；
            stringRedisTemplate.opsForValue().set(phoneNumber, stringBuilder.toString(),
                    2, TimeUnit.MINUTES);
            // 转换成为整数
            int smsCode = Integer.parseInt(stringBuilder.toString());
            return Result.getResult(ResultCode.SEND_CODE_SUCCESS.getCode(),
                    ResultCode.SEND_CODE_SUCCESS.getMessage(), smsCode);
        }
        return Result.getResult(ResultCode.SEND_CODE_FAILED.getCode(),
                ResultCode.SEND_CODE_FAILED.getMessage(), null);
    }

    // 短信验证码登录
    @Override
    public Result<Integer> smsLogin(String phoneNumber, String smsCodeInput) {
        // 从Redis中获取当前用户正确的短信验证码
        String realSmsCode = stringRedisTemplate.opsForValue().get(phoneNumber);

        // 验证码已过期
        if (realSmsCode == null) {
            return Result.getResult(ResultCode.SMSCODE_EXPIRED.getCode(),
                    ResultCode.SMSCODE_EXPIRED.getMessage(), null);
        }

        // 验证码正确
        if (realSmsCode.equals(smsCodeInput)) {
            return Result.getResult(ResultCode.SMS_LOGIN_SUCCESS.getCode(),
                    ResultCode.SMS_LOGIN_SUCCESS.getMessage(), null);
        }

        // 验证码错误
        return Result.getResult(ResultCode.SMS_LOGIN_FAILED.getCode(),
                ResultCode.SMS_LOGIN_FAILED.getMessage(), null);
    }
}
