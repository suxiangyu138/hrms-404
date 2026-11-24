package com.hrms404.controller;

import com.hrms404.common.AccountCreateRequest;
import com.hrms404.common.PageResult;
import com.hrms404.common.Result;
import com.hrms404.service.UserAccountService;
import com.hrms404.vo.EmpCandidateVO;
import com.hrms404.vo.UserAccountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 账号管理 RESTful 接口：/api/users
 * 「注册」在本系统中即 HR/管理员为员工开通登录账号
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    /** 账号分页查询 */
    @GetMapping
    public Result<PageResult<UserAccountVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Integer enabled) {
        return Result.success(userAccountService.page(page, size, keyword, roleCode, enabled));
    }

    /** 开通账号弹窗的员工候选 */
    @GetMapping("/candidates")
    public Result<List<EmpCandidateVO>> candidates(@RequestParam(required = false) String keyword) {
        return Result.success(userAccountService.candidates(keyword));
    }

    /**
     * 开通账号（注册）
     * 注：写接口统一返回非空 data —— 前端 api() 用「返回 null」表示失败，
     * data 为 null 的成功响应会被误判，故这里回传实际登录名。
     */
    @PostMapping
    public Result<String> create(@RequestBody AccountCreateRequest req) {
        String username = userAccountService.create(
                req.getUsername(), req.getPassword(), req.getRoleCode(), req.getEmpId());
        return Result.success("账号「" + username + "」开通成功", username);
    }

    /** 修改角色 */
    @PutMapping("/{id}/role")
    public Result<String> changeRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userAccountService.changeRole(id, body.get("roleCode"));
        return Result.success("角色已更新", body.get("roleCode"));
    }

    /** 重置密码（不传 newPassword 则重置为默认密码） */
    @PutMapping("/{id}/password")
    public Result<String> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        userAccountService.resetPassword(id, newPassword);
        return Result.success("密码已重置", newPassword == null || newPassword.isBlank()
                ? UserAccountService.DEFAULT_PASSWORD : newPassword);
    }

    /** 启用 / 禁用 */
    @PutMapping("/{id}/enabled")
    public Result<Integer> changeEnabled(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer enabled = body.get("enabled");
        userAccountService.changeEnabled(id, enabled);
        return Result.success(enabled != null && enabled == 0 ? "账号已禁用" : "账号已启用", enabled);
    }
}
