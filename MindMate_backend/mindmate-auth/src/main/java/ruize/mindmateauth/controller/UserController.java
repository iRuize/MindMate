package ruize.mindmateauth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ruize.mindmateauth.service.UserService;

import java.util.Random;

@RestController
@RequestMapping("/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/sendSmsCode")
    //前端传递“phoneNumber”
    public int SentSmsCode(@RequestParam("phoneNumber") String phoneNumber) {
        userService.sendSmsCode(phoneNumber);
        return 1;
    }

    @RequestMapping("/userLogin")
    public int userLogin(String username, String smsCode) {
        return 0;
    }

}
