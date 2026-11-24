package com.hrms404.vo;

import lombok.Data;

/**
 * 部门组织树节点（sp_get_org_tree 存储过程返回）
 */
@Data
public class DeptOrgNode {

    private Long deptId;
    private String deptName;
    private Long parentId;
    private Integer headcountBudget;
    /** 层级：0 为顶级 */
    private Integer deptLevel;
    /** 部门全路径：总公司 / 研发部 */
    private String deptPath;
}
