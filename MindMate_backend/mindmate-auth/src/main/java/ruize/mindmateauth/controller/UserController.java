package ruize.mindmateauth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ruize.mindmateauth.context.UserContext;
import ruize.mindmateauth.dto.LoginRequest;
import ruize.mindmateauth.dto.LoginResponse;
import ruize.mindmateauth.dto.UserVO;
import ruize.mindmateauth.result.Result;
import ruize.mindmateauth.service.UserService;

/**
 * 认证接口。
 * /sendSmsCode 与 /login 由拦截器放行，其余需携带 Authorization: Bearer {token}。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
@RestController
@RequestMapping("/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 发送验证码。伪装实现，验证码随响应体返回，不真实下发短信。
     */
    @GetMapping("/sendSmsCode")
    public Result<String> sendSmsCode(@RequestParam("phoneNumber") String phoneNumber) {
        return Result.success(userService.sendSmsCode(phoneNumber));
    }

    /**
     * 验证码登录。成功后返回登录凭证与用户信息。
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(userService.login(request.getPhoneNumber(), request.getSmsCode()));
    }

    /**
     * 查询当前登录用户。
     */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.getCurrentUser(UserContext.getUserId()));
    }

    /**
     * 退出登录。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout(UserContext.getToken());
        return Result.success();
    }
}
