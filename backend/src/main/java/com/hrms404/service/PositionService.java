package com.hrms404.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms404.common.BizException;
import com.hrms404.common.Roles;
import com.hrms404.entity.Employee;
import com.hrms404.entity.Position;
import com.hrms404.mapper.EmployeeMapper;
import com.hrms404.mapper.PositionMapper;
import com.hrms404.vo.PositionInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 职位业务：列表（含编制占用）、CRUD —— 新增/修改会触发 trg_check_budget 编制预算校验
 */
@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionMapper positionMapper;
    private final EmployeeMapper employeeMapper;

    public List<PositionInfoVO> listAll() {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        return positionMapper.selectPositionInfos();
    }

    @Transactional
    public void create(Position p) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        validate(p);
        positionMapper.insert(p);   // 超出部门编制预算时触发器 SIGNAL，全局异常转 409
    }

    @Transactional
    public void update(Long id, Position p) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        if (positionMapper.selectById(id) == null) {
            throw BizException.notFound("404：职位不存在");
        }
        validate(p);
        p.setPositionId(id);
        positionMapper.updateById(p);   // 同样经过触发器校验
    }

    @Transactional
    public void delete(Long id) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        if (positionMapper.selectById(id) == null) {
            throw BizException.notFound("404：职位不存在");
        }
        if (employeeMapper.selectCount(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getPositionId, id)) > 0) {
            throw BizException.conflict("该职位下仍有员工在职，无法删除（先调整员工职位）");
        }
        positionMapper.deleteById(id);
    }

    private void validate(Position p) {
        if (p.getPositionName() == null || p.getPositionName().isBlank()) {
            throw BizException.badRequest("职位名称不能为空");
        }
        if (p.getDeptId() == null) {
            throw BizException.badRequest("请选择所属部门");
        }
        if (p.getHeadcount() == null || p.getHeadcount() <= 0) {
            throw BizException.badRequest("编制数必须为正整数");
        }
        if (p.getBaseSalary() == null || p.getBaseSalary().signum() < 0) {
            throw BizException.badRequest("基本工资不能为负数");
        }
        p.setPositionName(p.getPositionName().trim());
    }
}
