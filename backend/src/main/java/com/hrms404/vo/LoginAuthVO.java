package com.hrms404.vo;

import lombok.Data;

/**
 * 登录认证行（单次 JOIN 查询结果）
 * 一次性取齐「账号 + 员工 + 部门 + 职位」，避免登录时多次往返数据库
 */
@Data
public class LoginAuthVO {

    // ---- sys_user ----
    private Long userId;
    private Long empId;
    private String username;
    private String password;
    private String roleCode;
    private Integer enabled;

    // ---- employee / department / position ----
    private String empName;
    private Long deptId;
    private String deptName;
    private String positionName;
}
