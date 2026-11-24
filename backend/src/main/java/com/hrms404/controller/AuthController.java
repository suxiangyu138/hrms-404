package com.hrms404.controller;

import com.hrms404.common.Result;
import com.hrms404.common.UserContext;
import com.hrms404.service.AuthService;
import com.hrms404.vo.LoginVO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 登录/登出/改密接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录（成功后写入会话） */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody Map<String, String> body, HttpSession session) {
        return Result.success("登录成功", authService.login(body.get("username"), body.get("password"), session));
    }

    /** 登出 */
    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        authService.logout(session);
        return Result.success("已退出登录", null);
    }

    /** 当前登录用户信息 */
    @GetMapping("/me")
    public Result<LoginVO> me() {
        return Result.success(authService.currentToVO(UserContext.get()));
    }

    /** 修改当前用户密码 */
    @PostMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        authService.changePassword(body.get("oldPassword"), body.get("newPassword"), UserContext.get());
        return Result.success("密码修改成功，下次登录请使用新密码", null);
    }
}
