package com.hrms404.vo;

import lombok.Data;

/**
 * 登录成功返回的用户信息（不含密码等敏感字段）
 */
@Data
public class LoginVO {

    private Long userId;
    private Long empId;
    private String username;
    private String empName;
    private String roleCode;
    private String roleName;
    private String deptName;
    private String positionName;
}
