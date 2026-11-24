package com.hrms404.common;

import java.util.Set;

/**
 * 系统角色常量与权限判定工具（RBAC）
 * ADMIN 系统管理员 / HR 人事专员 / MANAGER 部门经理 / EMPLOYEE 普通员工
 */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String HR = "HR";
    public static final String MANAGER = "MANAGER";
    public static final String EMPLOYEE = "EMPLOYEE";

    private Roles() {
    }

    /** 校验当前登录用户角色是否在允许集合内，否则抛 403 */
    public static void require(Set<String> allowed) {
        if (allowed == null || allowed.isEmpty()) {
            return;
        }
        String role = UserContext.role();
        if (role == null || !allowed.contains(role)) {
            throw BizException.forbidden("403：当前角色无权执行该操作");
        }
    }

    public static Set<String> of(String... roles) {
        return Set.of(roles);
    }
}
