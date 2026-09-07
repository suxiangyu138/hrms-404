package com.hrms404.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms404.common.*;
import com.hrms404.entity.Employee;
import com.hrms404.entity.Position;
import com.hrms404.mapper.DepartmentMapper;
import com.hrms404.mapper.EmployeeInfoMapper;
import com.hrms404.mapper.EmployeeMapper;
import com.hrms404.mapper.PositionMapper;
import com.hrms404.vo.EmployeeInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 员工业务：分页查询（经理按部门子树过滤）、新增/修改、离职（逻辑删除，触发器联动禁用账号）
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final EmployeeInfoMapper employeeInfoMapper;
    private final PositionMapper positionMapper;
    private final DepartmentMapper departmentMapper;

    /** 员工分页查询（MANAGER 自动限制到本部门子树） */
    public PageResult<EmployeeInfoVO> page(int page, int size, String keyword, Long deptId, Integer status) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR, Roles.MANAGER));

        LambdaQueryWrapper<EmployeeInfoVO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(EmployeeInfoVO::getEmpName, keyword.trim())
                    .or().like(EmployeeInfoVO::getEmpNo, keyword.trim()));
        }
        if (status != null) {
            wrapper.eq(EmployeeInfoVO::getStatus, status);
        }

        LoginSession user = UserContext.get();
        if (ScopeUtil.managerScoped(user)) {
            List<Long> deptIds = ScopeUtil.scopedDeptIds(departmentMapper, user);
            if (deptIds == null || deptIds.isEmpty()) {
                return PageResult.of(List.of(), 0, page, size);
            }
            wrapper.in(EmployeeInfoVO::getDeptId, deptIds);
        } else if (deptId != null) {
            wrapper.eq(EmployeeInfoVO::getDeptId, deptId);
        }
        wrapper.orderByDesc(EmployeeInfoVO::getEmpId);

        return PageResult.of(employeeInfoMapper.selectPage(Page.of(page, size), wrapper));
    }

    /** 按筛选条件返回全部匹配员工（导出用，不受分页限制；MANAGER 自动限制部门子树） */
    public List<EmployeeInfoVO> listAll(String keyword, Long deptId, Integer status) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR, Roles.MANAGER));

        LambdaQueryWrapper<EmployeeInfoVO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(EmployeeInfoVO::getEmpName, keyword.trim())
                    .or().like(EmployeeInfoVO::getEmpNo, keyword.trim()));
        }
        if (status != null) {
            wrapper.eq(EmployeeInfoVO::getStatus, status);
        }

        LoginSession user = UserContext.get();
        if (ScopeUtil.managerScoped(user)) {
            List<Long> deptIds = ScopeUtil.scopedDeptIds(departmentMapper, user);
            if (deptIds == null || deptIds.isEmpty()) {
                return List.of();
            }
            wrapper.in(EmployeeInfoVO::getDeptId, deptIds);
        } else if (deptId != null) {
            wrapper.eq(EmployeeInfoVO::getDeptId, deptId);
        }
        wrapper.orderByAsc(EmployeeInfoVO::getEmpId);
        return employeeInfoMapper.selectList(wrapper);
    }

    /** 员工详情（视图）；MANAGER 只能查看本部门子树内的员工 */
    public EmployeeInfoVO detail(Long id) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR, Roles.MANAGER));
        LoginSession user = UserContext.get();
        if (ScopeUtil.managerScoped(user)) {
            List<Long> scoped = ScopeUtil.scopedEmpIds(departmentMapper, employeeMapper, user);
            if (scoped == null || !scoped.contains(id)) {
                throw BizException.notFound("404：员工不存在（数据范围之外）");
            }
        }
        EmployeeInfoVO vo = employeeInfoMapper.selectById(id);
        if (vo == null) {
            throw BizException.notFound("404：员工不存在（已从列表 404）");
        }
        return vo;
    }

    /** 当前登录员工本人的完整信息（个人中心；未绑定员工返回 null） */
    public EmployeeInfoVO myInfo() {
        Long empId = UserContext.empId();
        return empId == null ? null : employeeInfoMapper.selectById(empId);
    }

    /** 新增员工（HR/管理员；不填工号自动生成 E0001 格式） */
    @Transactional
    public Employee create(Employee e) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        validate(e);
        if (e.getEmpNo() == null || e.getEmpNo().isBlank()) {
            int next = (employeeMapper.maxEmpSeq() == null ? 0 : employeeMapper.maxEmpSeq()) + 1;
            e.setEmpNo(String.format("E%04d", next));
        }
        e.setStatus(1);
        employeeMapper.insert(e);
        return e;
    }

    /** 修改员工信息 */
    @Transactional
    public void update(Long id, Employee e) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        Employee exist = employeeMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("404：员工不存在（已从列表 404）");
        }
        validate(e);
        e.setEmpId(id);
        e.setStatus(exist.getStatus());   // 状态不允许在此接口变更
        employeeMapper.updateById(e);
    }

    /** 员工离职/复职：更新 status 字段，离职触发器自动禁用其账号 */
    @Transactional
    public void changeStatus(Long id, Integer status) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        if (status == null || (status != 0 && status != 1)) {
            throw BizException.badRequest("status 仅支持 0（离职）/1（在职）");
        }
        Employee exist = employeeMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("404：员工不存在（已从列表 404）");
        }
        Employee update = new Employee();
        update.setEmpId(id);
        update.setStatus(status);
        employeeMapper.updateById(update);
    }

    /** 某部门下的职位（表单联动下拉） */
    public List<Position> positionsOfDept(Long deptId) {
        return positionMapper.selectList(new LambdaQueryWrapper<Position>()
                .eq(Position::getDeptId, deptId)
                .orderByAsc(Position::getPositionId));
    }

    private void validate(Employee e) {
        if (e.getEmpName() == null || e.getEmpName().isBlank()) {
            throw BizException.badRequest("姓名不能为空");
        }
        if (e.getHireDate() == null) {
            throw BizException.badRequest("入职日期不能为空");
        }
        if (e.getDeptId() == null || e.getPositionId() == null) {
            throw BizException.badRequest("请选择部门和职位");
        }
        if (e.getBirthDate() != null && e.getBirthDate().isAfter(LocalDate.now())) {
            throw BizException.badRequest("出生日期不能晚于今天");
        }
        if (e.getEmpNo() != null && !e.getEmpNo().matches("[A-Za-z][A-Za-z0-9]{0,19}")) {
            throw BizException.badRequest("工号须以字母开头，长度不超过 20");
        }
    }
}
