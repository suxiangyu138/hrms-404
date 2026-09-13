package com.hrms404.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账号列表行（sys_user + employee + department 联查结果）
 * 刻意不含 password 字段，避免密码散列随接口外泄
 */
@Data
public class UserAccountVO {

    private Long userId;
    private Long empId;
    private String username;

    /** 绑定的员工信息（管理员账号可为空） */
    private String empNo;
    private String empName;
    private String deptName;

    private String roleCode;
    private Integer enabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createdAt;
}
