package com.hrms404.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms404.entity.Department;
import com.hrms404.vo.DeptOrgNode;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 部门 Mapper：标准 CRUD + 存储过程调用（组织树/递归计数）
 */
public interface DepartmentMapper extends BaseMapper<Department> {

    /** 调用 sp_get_org_tree：递归返回部门组织树（含层级与全路径） */
    @Select("CALL sp_get_org_tree()")
    List<DeptOrgNode> selectOrgTree();

    /** 调用 sp_count_employee_by_dept：统计指定部门及全部子部门在职人数 */
    @Select("CALL sp_count_employee_by_dept(#{deptId})")
    Long countDeptAndSubEmployees(@Param("deptId") Long deptId);

    /** 递归查询指定部门及其全部子部门ID（数据范围控制用） */
    @Select("""
            WITH RECURSIVE dept_tree AS (
                SELECT dept_id FROM department WHERE dept_id = #{deptId}
                UNION ALL
                SELECT d.dept_id
                FROM department d
                         INNER JOIN dept_tree t ON d.parent_id = t.dept_id
            )
            SELECT dept_id FROM dept_tree
            """)
    List<Long> selectChildDeptIds(@Param("deptId") Long deptId);
}
