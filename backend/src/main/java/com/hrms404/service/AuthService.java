package com.hrms404.service;

import com.hrms404.common.BizException;
import com.hrms404.common.LoginSession;
import com.hrms404.entity.SysUser;
import com.hrms404.mapper.SysUserMapper;
import com.hrms404.security.AuthInterceptor;
import com.hrms404.security.PasswordUtil;
import com.hrms404.vo.LoginAuthVO;
import com.hrms404.vo.LoginVO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 登录认证服务：MD5+盐校验、构建会话、登出
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;

    /**
     * 登录：1 次 JOIN 查询取齐「账号 + 员工 + 部门 + 职位」，校验通过后写入会话。
     * 校验顺序保持为 账号存在 → 已启用 → 密码正确，便于前端提示区分。
     */
    public LoginVO login(String username, String password, HttpSession session) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw BizException.badRequest("请输入账号和密码");
        }
        LoginAuthVO row = sysUserMapper.selectLoginByUsername(username.trim());
        if (row == null) {
            throw BizException.badRequest("账号不存在");
        }
        if (row.getEnabled() == null || row.getEnabled() != 1) {
            throw BizException.badRequest("账号已被禁用（可能已离职），请联系 HR");
        }
        if (!PasswordUtil.matches(password, row.getPassword())) {
            throw BizException.badRequest("密码错误，请重试");
        }

        // 组装会话信息（部门/职位名称已由 JOIN 一次带出）
        LoginSession login = new LoginSession();
        login.setUserId(row.getUserId());
        login.setEmpId(row.getEmpId());
        login.setUsername(row.getUsername());
        login.setRoleCode(row.getRoleCode());
        login.setRoleName(LoginSession.roleNameOf(row.getRoleCode()));
        login.setEmpName(row.getEmpName());
        login.setDeptId(row.getDeptId());
        login.setDeptName(row.getDeptName());
        login.setPositionName(row.getPositionName());

        session.setAttribute(AuthInterceptor.SESSION_KEY, login);
        return toVO(login);
    }

    public void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    public LoginVO currentToVO(LoginSession login) {
        return toVO(login);
    }

    private LoginVO toVO(LoginSession login) {
        LoginVO vo = new LoginVO();
        vo.setUserId(login.getUserId());
        vo.setEmpId(login.getEmpId());
        vo.setUsername(login.getUsername());
        vo.setEmpName(login.getEmpName());
        vo.setRoleCode(login.getRoleCode());
        vo.setRoleName(LoginSession.roleNameOf(login.getRoleCode()));
        vo.setDeptName(login.getDeptName());
        vo.setPositionName(login.getPositionName());
        return vo;
    }

    /** 修改当前用户密码（附加功能） */
    public void changePassword(String oldPassword, String newPassword, LoginSession login) {
        SysUser user = sysUserMapper.selectById(login.getUserId());
        if (user == null) {
            throw BizException.notFound("账号不存在");
        }
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw BizException.badRequest("原密码错误");
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            throw BizException.badRequest("新密码长度须为 6~20 位");
        }
        SysUser update = new SysUser();
        update.setUserId(user.getUserId());
        update.setPassword(PasswordUtil.encrypt(newPassword));
        sysUserMapper.updateById(update);
    }
}
