package ruize.mindmateauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ruize.mindmateauth.entity.User;
import ruize.mindmateauth.enums.ResultCode;
import ruize.mindmateauth.mapper.UserMapper;
import ruize.mindmateauth.result.Result;
import ruize.mindmateauth.service.UserService;

import java.util.Random;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

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
            // 转换成为整数
            int smsCode = Integer.parseInt(stringBuilder.toString());
            return Result.getResult(ResultCode.SEND_CODE_SUCCESS.getCode(),
                    ResultCode.SEND_CODE_SUCCESS.getMessage(), smsCode);
        }
        return Result.getResult(ResultCode.SEND_CODE_FAILED.getCode(), 
                ResultCode.SEND_CODE_FAILED.getMessage(), null);
    }

    @Override
    public int login(String username, String password) {
        return 0;
    }
}
