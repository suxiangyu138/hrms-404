package com.hrms404.controller;

import com.hrms404.common.Result;
import com.hrms404.entity.Department;
import com.hrms404.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 部门 RESTful 接口：/api/departments（树形组织由存储过程递归返回）
 */
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /** 组织树 + 人数统计（树节点/直属人数/含子树总人数） */
    @GetMapping("/tree")
    public Result<Map<String, Object>> tree() {
        return Result.success(departmentService.treeWithCounts());
    }

    /** 调用存储过程 sp_count_employee_by_dept */
    @GetMapping("/{id}/count")
    public Result<Long> countWithSubs(@PathVariable Long id) {
        return Result.success(departmentService.countDeptAndSubEmployees(id));
    }

    @GetMapping("/{id}")
    public Result<Department> detail(@PathVariable Long id) {
        return Result.success(departmentService.detail(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Department department) {
        departmentService.create(department);
        return Result.success("新增部门成功", null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Department department) {
        departmentService.update(id, department);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return Result.success("部门已删除（它已从组织架构 404）", null);
    }
}
