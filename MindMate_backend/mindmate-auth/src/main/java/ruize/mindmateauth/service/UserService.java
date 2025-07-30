package ruize.mindmateauth.service;

import org.springframework.web.bind.annotation.RequestParam;
import ruize.mindmateauth.result.Result;

public interface UserService {
    Result<Integer> sendSmsCode(String phoneNumber);
    Result<Integer> smsLogin(String phoneNumber, String smsCodeInput);
}
