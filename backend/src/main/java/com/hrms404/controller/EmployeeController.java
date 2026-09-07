package com.hrms404.controller;

import com.hrms404.common.PageResult;
import com.hrms404.common.Result;
import com.hrms404.entity.Employee;
import com.hrms404.entity.Position;
import com.hrms404.service.EmployeeService;
import com.hrms404.vo.EmployeeInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 员工 RESTful 接口：/api/employees
 */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /** 分页查询（MANAGER 数据范围由服务层限制为本部门子树） */
    @GetMapping
    public Result<PageResult<EmployeeInfoVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status) {
        return Result.success(employeeService.page(page, size, keyword, deptId, status));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<EmployeeInfoVO> detail(@PathVariable Long id) {
        return Result.success(employeeService.detail(id));
    }

    /** 新增（自动生成工号） */
    @PostMapping
    public Result<Employee> create(@RequestBody Employee employee) {
        return Result.success("新增员工成功（登录账号=工号，默认密码 123456）", employeeService.create(employee));
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Employee employee) {
        employeeService.update(id, employee);
        return Result.success("修改成功", null);
    }

    /** 离职（status=0）/复职（status=1）——逻辑删除，触发器联动账号 */
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        employeeService.changeStatus(id, body.get("status"));
        Integer status = body.get("status");
        return Result.success(status != null && status == 0 ? "已办理离职（账号自动禁用）" : "已办理复职", null);
    }

    /** 部门下的职位（新增/编辑员工表单联动） */
    @GetMapping("/position-options")
    public Result<List<Position>> positionOptions(@RequestParam Long deptId) {
        return Result.success(employeeService.positionsOfDept(deptId));
    }
}
