package com.hrms404.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms404.common.BizException;
import com.hrms404.common.Roles;
import com.hrms404.entity.Department;
import com.hrms404.entity.Employee;
import com.hrms404.mapper.DepartmentMapper;
import com.hrms404.mapper.EmployeeMapper;
import com.hrms404.vo.DeptOrgNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门业务：组织树（存储过程递归）+ 编制人数统计（存储过程）+ CRUD
 */
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;

    /** 组织树节点 + 每个部门的人数统计（直属人数 direct、含子树 total） */
    public Map<String, Object> treeWithCounts() {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));

        List<DeptOrgNode> tree = departmentMapper.selectOrgTree();
        // 直属人数：按部门分组统计在职员工
        Map<Long, Long> direct = new HashMap<>();
        employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getStatus, 1)
                        .select(Employee::getDeptId))
                .forEach(e -> direct.merge(e.getDeptId(), 1L, Long::sum));
        // 含子部门总人数：先各自取直属数，再由子向上累加到父。
        // selectOrgTree 按 dept_path 排序，父路径必为子路径前缀，故父一定排在子之前；
        // 倒序遍历即为「子先于父」，可一次线性累加完成，无需递归。
        // 注意方向：是「父 += 子」，不能写成「子 += 父」——后者读到的是父节点尚未累加的
        // 初始值（0），会让所有非叶子部门恒为 0，而叶子因父值为 0 恰好等于直属数、看不出错。
        Map<Long, Long> total = new HashMap<>();
        for (DeptOrgNode node : tree) {
            total.put(node.getDeptId(), direct.getOrDefault(node.getDeptId(), 0L));
        }
        for (int i = tree.size() - 1; i >= 0; i--) {
            DeptOrgNode node = tree.get(i);
            if (node.getParentId() != null) {
                total.merge(node.getParentId(), total.get(node.getDeptId()), Long::sum);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("tree", tree);
        result.put("directCounts", direct);
        result.put("subtreeCounts", total);
        return result;
    }

    /** 调用存储过程：统计某部门及全部子部门在职人数 */
    public Long countDeptAndSubEmployees(Long deptId) {
        return departmentMapper.countDeptAndSubEmployees(deptId);
    }

    public Department detail(Long id) {
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw BizException.notFound("404：部门不存在");
        }
        return dept;
    }

    @Transactional
    public void create(Department d) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        if (d.getDeptName() == null || d.getDeptName().isBlank()) {
            throw BizException.badRequest("部门名称不能为空");
        }
        checkParent(d.getParentId());
        if (departmentMapper.selectCount(new LambdaQueryWrapper<Department>()
                .eq(Department::getDeptName, d.getDeptName().trim())) > 0) {
            throw BizException.conflict("部门名称已存在");
        }
        d.setDeptName(d.getDeptName().trim());
        departmentMapper.insert(d);
    }

    @Transactional
    public void update(Long id, Department d) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        detail(id);
        if (d.getDeptName() == null || d.getDeptName().isBlank()) {
            throw BizException.badRequest("部门名称不能为空");
        }
        if (id.equals(d.getParentId())) {
            throw BizException.conflict("上级部门不能是自身");
        }
        checkParent(d.getParentId());
        d.setDeptId(id);
        d.setDeptName(d.getDeptName().trim());
        departmentMapper.updateById(d);
    }

    /** 删除部门：有子部门或有员工时禁止删除 */
    @Transactional
    public void delete(Long id) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        detail(id);
        if (departmentMapper.selectCount(new LambdaQueryWrapper<Department>()
                .eq(Department::getParentId, id)) > 0) {
            throw BizException.conflict("该部门下存在子部门，请先移动或删除子部门");
        }
        if (employeeMapper.selectCount(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getDeptId, id)) > 0) {
            throw BizException.conflict("该部门下存在员工，无法删除");
        }
        departmentMapper.deleteById(id);
    }

    private void checkParent(Long parentId) {
        if (parentId == null) {
            return;
        }
        if (departmentMapper.selectById(parentId) == null) {
            throw BizException.badRequest("上级部门不存在");
        }
    }
}
