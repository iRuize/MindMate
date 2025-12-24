package ruize.mindmateauth.controller;

import org.springframework.web.bind.annotation.*;
import ruize.mindmateauth.result.Result;
import ruize.mindmateauth.service.UserService;

@RestController
@RequestMapping("/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/sendSmsCode")
    // 前端传递“phoneNumber”
    public Result<Integer> sendSmsCode(@RequestParam("phoneNumber") String phoneNumber) {
        Result<Integer> result = userService.sendSmsCode(phoneNumber);
        if (result.getCode() == 201) {
            System.out.println(result.getData());
            return result;
        }
        return result;
    }

    @PostMapping("/userSmsLogin")
    public Result<Integer> smsLogin(String phoneNumber, String smsCode) {
        return userService.smsLogin(phoneNumber, smsCode);
    }

}
