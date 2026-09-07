package com.hrms404.service;

import com.hrms404.common.BizException;
import com.hrms404.common.LoginSession;
import com.hrms404.entity.Department;
import com.hrms404.entity.Employee;
import com.hrms404.entity.Position;
import com.hrms404.entity.SysUser;
import com.hrms404.mapper.DepartmentMapper;
import com.hrms404.mapper.EmployeeMapper;
import com.hrms404.mapper.PositionMapper;
import com.hrms404.mapper.SysUserMapper;
import com.hrms404.security.AuthInterceptor;
import com.hrms404.security.PasswordUtil;
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
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final PositionMapper positionMapper;

    public LoginVO login(String username, String password, HttpSession session) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw BizException.badRequest("请输入账号和密码");
        }
        SysUser user = sysUserMapper.selectByUsername(username.trim());
        if (user == null) {
            throw BizException.badRequest("账号不存在");
        }
        if (user.getEnabled() == null || user.getEnabled() != 1) {
            throw BizException.badRequest("账号已被禁用（可能已离职），请联系 HR");
        }
        if (!PasswordUtil.matches(password, user.getPassword())) {
            throw BizException.badRequest("密码错误，请重试");
        }

        // 组装会话信息（含所属部门/职位名称）
        LoginSession login = new LoginSession();
        login.setUserId(user.getUserId());
        login.setEmpId(user.getEmpId());
        login.setUsername(user.getUsername());
        login.setRoleCode(user.getRoleCode());
        login.setRoleName(LoginSession.roleNameOf(user.getRoleCode()));

        if (user.getEmpId() != null) {
            Employee emp = employeeMapper.selectById(user.getEmpId());
            if (emp != null) {
                login.setEmpName(emp.getEmpName());
                login.setDeptId(emp.getDeptId());
                Department dept = departmentMapper.selectById(emp.getDeptId());
                Position position = positionMapper.selectById(emp.getPositionId());
                login.setDeptName(dept == null ? null : dept.getDeptName());
                login.setPositionName(position == null ? null : position.getPositionName());
            }
        }
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
