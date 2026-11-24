package com.hrms404.vo;

import lombok.Data;

/**
 * 开通账号时的可选员工（在职员工 + 是否已有账号标记）
 */
@Data
public class EmpCandidateVO {

    private Long empId;
    private String empNo;
    private String empName;
    private String deptName;

    /** 1 已有账号，0 可开通（前端据此置灰） */
    private Integer hasAccount;
}
