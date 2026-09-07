package com.hrms404.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 职位列表信息（含部门名与在职占用编制数，用于编制利用率展示）
 */
@Data
public class PositionInfoVO {

    private Long positionId;
    private String positionName;
    private Long deptId;
    private String deptName;
    private Integer headcount;
    private Long usedCount;
    private BigDecimal baseSalary;
}
