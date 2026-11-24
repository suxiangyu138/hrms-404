package com.hrms404.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 薪资表（每员工每月一条，实发工资由触发器自动计算）
 */
@Data
@TableName("salary")
public class Salary {

    @TableId(type = IdType.AUTO)
    private Long salaryId;

    private Long empId;

    /** 发放月份 YYYY-MM */
    private String salaryMonth;

    /** 基本工资（岗位工资） */
    private BigDecimal baseSalary;

    /** 绩效奖金 */
    private BigDecimal performance;

    /** 扣款 */
    private BigDecimal deduction;

    /** 实发工资（触发器计算） */
    private BigDecimal actualSalary;

    /** 生成时间 */
    private LocalDateTime createdAt;
}
