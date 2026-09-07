package com.hrms404.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 薪资历史（v_salary_history 只读视图）
 */
@Data
@TableName("v_salary_history")
public class SalaryInfoVO {

    @TableId
    private Long salaryId;

    private Long empId;
    private String empNo;
    private String empName;
    private String deptName;
    private String salaryMonth;
    private BigDecimal baseSalary;
    private BigDecimal performance;
    private BigDecimal deduction;
    private BigDecimal actualSalary;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createdAt;
}
