package com.hrms404.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表（登录账号；与员工 1:1，管理员可不绑定员工）
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long userId;

    /** 关联员工ID（1:1，可空） */
    private Long empId;

    /** 登录名 */
    private String username;

    /** 密码：MD5(盐+密码)，盐=404n0tf0und */
    private String password;

    /** 角色：ADMIN/HR/MANAGER/EMPLOYEE */
    private String roleCode;

    /** 1 启用 0 禁用（离职触发器自动禁用） */
    private Integer enabled;

    private LocalDateTime createdAt;
}
