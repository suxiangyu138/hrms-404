package com.hrms404.common;

import lombok.Data;

/**
 * 开通账号请求体
 */
@Data
public class AccountCreateRequest {

    /** 关联员工编号；为空表示创建不绑定员工的管理员账号 */
    private Long empId;

    /** 登录名；为空时默认取员工工号 */
    private String username;

    /** 初始密码；为空时默认 123456 */
    private String password;

    /** 角色：ADMIN/HR/MANAGER/EMPLOYEE；为空时默认 EMPLOYEE */
    private String roleCode;
}
