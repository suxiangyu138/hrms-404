package com.hrms404.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 职位表
 */
@Data
@TableName("position")
public class Position {

    @TableId(type = IdType.AUTO)
    private Long positionId;

    /** 职位名称 */
    private String positionName;

    /** 所属部门 */
    private Long deptId;

    /** 编制数 */
    private Integer headcount;

    /** 岗位基本工资（元/月） */
    private BigDecimal baseSalary;
}
