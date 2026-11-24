package com.hrms404.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms404.entity.Employee;
import com.hrms404.mapper.DepartmentMapper;
import com.hrms404.mapper.EmployeeMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据范围工具：MANAGER 角色按"所属部门子树"过滤；其余角色范围为全库
 * 返回 null 表示无需过滤（全库范围）
 */
public final class ScopeUtil {

    private ScopeUtil() {
    }

    public static boolean managerScoped(LoginSession user) {
        return Roles.MANAGER.equals(user.getRoleCode()) && user.getDeptId() != null;
    }

    /** 经理可见部门ID集合（含自身部门与全部子部门） */
    public static List<Long> scopedDeptIds(DepartmentMapper departmentMapper, LoginSession user) {
        if (!managerScoped(user)) {
            return null;
        }
        return departmentMapper.selectChildDeptIds(user.getDeptId());
    }

    /** 经理可见范围内的员工ID集合 */
    public static List<Long> scopedEmpIds(DepartmentMapper departmentMapper,
                                          EmployeeMapper employeeMapper,
                                          LoginSession user) {
        List<Long> deptIds = scopedDeptIds(departmentMapper, user);
        if (deptIds == null) {
            return null;
        }
        List<Long> empIds = new ArrayList<>();
        if (!deptIds.isEmpty()) {
            employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                            .select(Employee::getEmpId)
                            .in(Employee::getDeptId, deptIds))
                    .forEach(e -> empIds.add(e.getEmpId()));
        }
        return empIds;
    }
}
