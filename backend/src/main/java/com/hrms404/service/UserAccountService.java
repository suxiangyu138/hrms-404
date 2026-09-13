package com.hrms404.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms404.common.*;
import com.hrms404.entity.Employee;
import com.hrms404.entity.SysUser;
import com.hrms404.mapper.EmployeeMapper;
import com.hrms404.mapper.SysUserMapper;
import com.hrms404.mapper.UserAccountMapper;
import com.hrms404.security.PasswordUtil;
import com.hrms404.vo.EmpCandidateVO;
import com.hrms404.vo.UserAccountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 账号管理（课程项目中的「注册」入口）：开通账号、改角色、重置密码、启用/禁用。
 *
 * 权限约定：
 * - ADMIN / HR 可进入本模块，其它角色 403；
 * - HR 仅可授予「普通员工 / 部门经理」，且不可操作管理员账号（防止越权提权）；
 * - 任何账号都不能修改自己的角色或禁用自己。
 *
 * 由此「系统始终至少有一名可用管理员」是自然成立的：能改动管理员账号的只能是另一名
 * 已登录的管理员，而任何人都动不了自己，故不存在把最后一名管理员降级/禁用的路径。
 */
@Service
@RequiredArgsConstructor
public class UserAccountService {

    /** 未指定初始密码时使用（与建库触发器、演示账号保持一致） */
    public static final String DEFAULT_PASSWORD = "123456";

    private static final Set<String> VALID_ROLES =
            Set.of(Roles.ADMIN, Roles.HR, Roles.MANAGER, Roles.EMPLOYEE);
    private static final Pattern USERNAME_RE = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    private final UserAccountMapper userAccountMapper;
    private final SysUserMapper sysUserMapper;
    private final EmployeeMapper employeeMapper;

    // ==================== 查询 ====================

    /** 账号分页查询 */
    public PageResult<UserAccountVO> page(int page, int size, String keyword, String roleCode, Integer enabled) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        String role = blankToNull(roleCode);
        if (role != null) {
            role = normalizeRole(role);
        }
        Page<UserAccountVO> result = userAccountMapper.selectAccountPage(
                Page.of(page, size), blankToNull(keyword), role, enabled);
        return PageResult.of(result);
    }

    /** 开通账号弹窗的员工候选（关键字为空时返回前 20 位在职员工） */
    public List<EmpCandidateVO> candidates(String keyword) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        return userAccountMapper.searchCandidates(blankToNull(keyword));
    }

    // ==================== 开通账号（注册） ====================

    /**
     * 为员工开通登录账号；empId 为空表示创建不绑定员工的管理员账号。
     * 登录名默认取工号，初始密码默认 123456。
     *
     * @return 实际使用的登录名
     */
    @Transactional
    public String create(String username, String password, String roleCode, Long empId) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        String role = normalizeRole(roleCode);
        requireGrantable(role);

        Employee emp = null;
        if (empId != null) {
            emp = employeeMapper.selectById(empId);
            if (emp == null) {
                throw BizException.notFound("员工不存在");
            }
            if (emp.getStatus() != null && emp.getStatus() == 0) {
                throw BizException.badRequest("该员工已离职，无法开通账号");
            }
            if (sysUserMapper.countByEmpId(empId) > 0) {
                throw BizException.conflict("该员工已有登录账号，请勿重复开通");
            }
        }

        String name = blankToNull(username);
        if (name == null) {
            if (emp == null) {
                throw BizException.badRequest("不绑定员工时必须填写登录名");
            }
            name = emp.getEmpNo();
        }
        validateUsername(name);
        // uk_user_name 唯一索引兜底，这里先行校验以便返回友好提示
        if (sysUserMapper.selectByUsername(name) != null) {
            throw BizException.conflict("登录名「" + name + "」已被占用");
        }

        String rawPassword = blankToNull(password) == null ? DEFAULT_PASSWORD : password;
        validatePassword(rawPassword);

        SysUser user = new SysUser();
        user.setEmpId(empId);
        user.setUsername(name);
        user.setPassword(PasswordUtil.encrypt(rawPassword));
        user.setRoleCode(role);
        user.setEnabled(1);
        sysUserMapper.insert(user);
        return name;
    }

    // ==================== 变更 ====================

    /** 修改账号角色 */
    @Transactional
    public void changeRole(Long userId, String roleCode) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        String role = normalizeRole(roleCode);
        requireGrantable(role);

        SysUser target = requireUser(userId);
        requireManageable(target);
        if (target.getUserId().equals(currentUserId())) {
            throw BizException.badRequest("不能修改自己的角色");
        }
        if (role.equals(target.getRoleCode())) {
            return;
        }
        updateField(userId, u -> u.setRoleCode(role));
    }

    /** 重置密码（不传新密码则重置为默认密码 123456） */
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        SysUser target = requireUser(userId);
        requireManageable(target);

        String rawPassword = blankToNull(newPassword) == null ? DEFAULT_PASSWORD : newPassword;
        validatePassword(rawPassword);
        updateField(userId, u -> u.setPassword(PasswordUtil.encrypt(rawPassword)));
    }

    /** 启用 / 禁用账号 */
    @Transactional
    public void changeEnabled(Long userId, Integer enabled) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw BizException.badRequest("状态值不合法");
        }
        SysUser target = requireUser(userId);
        requireManageable(target);
        if (target.getUserId().equals(currentUserId())) {
            throw BizException.badRequest("不能禁用当前登录的账号");
        }
        updateField(userId, u -> u.setEnabled(enabled));
    }

    // ==================== 内部工具 ====================

    private SysUser requireUser(Long userId) {
        SysUser user = userId == null ? null : sysUserMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("账号不存在");
        }
        return user;
    }

    /** 按需更新单个字段：只 set 目标字段，避免把 password 等无关列写回 */
    private void updateField(Long userId, java.util.function.Consumer<SysUser> setter) {
        SysUser update = new SysUser();
        update.setUserId(userId);
        setter.accept(update);
        sysUserMapper.updateById(update);
    }

    private Long currentUserId() {
        LoginSession me = UserContext.get();
        return me == null ? null : me.getUserId();
    }

    /** 角色白名单校验，缺省为普通员工 */
    private String normalizeRole(String roleCode) {
        String role = blankToNull(roleCode);
        if (role == null) {
            return Roles.EMPLOYEE;
        }
        role = role.toUpperCase(Locale.ROOT);
        if (!VALID_ROLES.contains(role)) {
            throw BizException.badRequest("角色不合法");
        }
        return role;
    }

    /** HR 不得授予管理员/人事角色 */
    private void requireGrantable(String role) {
        if (Roles.HR.equals(UserContext.role()) && (Roles.ADMIN.equals(role) || Roles.HR.equals(role))) {
            throw BizException.forbidden("403：人事专员仅可授予「普通员工 / 部门经理」角色");
        }
    }

    /** HR 不得操作管理员账号 */
    private void requireManageable(SysUser target) {
        if (Roles.HR.equals(UserContext.role()) && Roles.ADMIN.equals(target.getRoleCode())) {
            throw BizException.forbidden("403：人事专员无权操作管理员账号");
        }
    }

    private void validateUsername(String username) {
        if (!USERNAME_RE.matcher(username).matches()) {
            throw BizException.badRequest("登录名须为 3~30 位字母、数字或下划线");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 20) {
            throw BizException.badRequest("密码长度须为 6~20 位");
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
