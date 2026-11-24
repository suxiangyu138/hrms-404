package com.hrms404.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录会话信息（HttpSession 存储 + 请求级 ThreadLocal 传递）
 */
@Data
public class LoginSession implements Serializable {

    private Long userId;
    private Long empId;
    private String username;
    private String empName;
    private String roleCode;
    private String roleName;
    private Long deptId;
    private String deptName;
    private String positionName;

    public static String roleNameOf(String roleCode) {
        return switch (roleCode == null ? "" : roleCode) {
            case Roles.ADMIN -> "系统管理员";
            case Roles.HR -> "人事专员";
            case Roles.MANAGER -> "部门经理";
            case Roles.EMPLOYEE -> "普通员工";
            default -> roleCode == null ? "" : roleCode;
        };
    }
}
