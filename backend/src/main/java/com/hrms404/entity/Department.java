package com.hrms404.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 部门表（自关联树形）
 */
@Data
@TableName("department")
public class Department {

    @TableId(type = IdType.AUTO)
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 上级部门ID，NULL 为顶级 */
    private Long parentId;

    /** 部门编制预算上限（人数），NULL 不限 */
    private Integer headcountBudget;
}
