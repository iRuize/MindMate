package ruize.mindmateauth.service;

import ruize.mindmateauth.result.Result;

public interface UserService {
    Result<Integer> sendSmsCode(String phoneNumber);

    Result<Integer> smsLogin(String phoneNumber, String smsCodeInput);
}