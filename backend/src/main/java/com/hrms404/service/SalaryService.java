package com.hrms404.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms404.common.BizException;
import com.hrms404.common.Roles;
import com.hrms404.common.UserContext;
import com.hrms404.entity.Salary;
import com.hrms404.mapper.SalaryInfoMapper;
import com.hrms404.mapper.SalaryMapper;
import com.hrms404.vo.SalaryInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 薪资业务：存储过程批量生成、按月份查询、绩效/扣款调整（实发重算）
 */
@Service
@RequiredArgsConstructor
public class SalaryService {

    private final SalaryMapper salaryMapper;
    private final SalaryInfoMapper salaryInfoMapper;

    /** 按月份查询（ADMIN/HR 全量；MANAGER 被页面拦截，不进入本方法） */
    public List<SalaryInfoVO> listByMonth(String month) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        validateMonth(month);
        return salaryInfoMapper.selectList(new LambdaQueryWrapper<SalaryInfoVO>()
                .eq(SalaryInfoVO::getSalaryMonth, month)
                .orderByAsc(SalaryInfoVO::getEmpId));
    }

    /** 调用存储过程 sp_generate_monthly_salary 批量生成，返回新生成条数 */
    @Transactional
    public long generate(String month) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        validateMonth(month);
        List<Map<String, Object>> result = salaryMapper.callGenerateMonthly(month);
        if (result == null || result.isEmpty()) {
            return 0;
        }
        Object v = result.get(0).get("generated_count");
        return v == null ? 0 : Long.parseLong(String.valueOf(v));
    }

    /** 手工调整绩效/扣款，并复现实发工资计算规则（与触发器保持一致） */
    @Transactional
    public void adjust(Long id, BigDecimal performance, BigDecimal deduction) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR));
        Salary exist = salaryMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("404：薪资记录不存在");
        }
        if (performance != null && performance.signum() < 0 || deduction != null && deduction.signum() < 0) {
            throw BizException.badRequest("绩效与扣款不能为负数");
        }
        Salary update = new Salary();
        update.setSalaryId(id);
        update.setPerformance(performance == null ? exist.getPerformance() : performance);
        update.setDeduction(deduction == null ? exist.getDeduction() : deduction);
        // 与 trg_before_salary_insert 相同规则：实发 = 基本 + 绩效 - 扣款
        update.setActualSalary(exist.getBaseSalary()
                .add(update.getPerformance() == null ? BigDecimal.ZERO : update.getPerformance())
                .subtract(update.getDeduction() == null ? BigDecimal.ZERO : update.getDeduction()));
        salaryMapper.updateById(update);
    }

    /** 当前员工本人的薪资历史（个人中心） */
    public List<SalaryInfoVO> mySalaries() {
        Long empId = UserContext.empId();
        if (empId == null) {
            return List.of();
        }
        return salaryInfoMapper.selectList(new LambdaQueryWrapper<SalaryInfoVO>()
                .eq(SalaryInfoVO::getEmpId, empId)
                .orderByDesc(SalaryInfoVO::getSalaryMonth));
    }

    /** 某月某批员工的薪资总额（Dashboard 数据范围统计用，不限角色内部调用） */
    public BigDecimal payrollOf(List<Long> empIds, String month) {
        if (empIds == null || empIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Set<Long> allowed = Set.copyOf(empIds);
        return salaryInfoMapper.selectList(new LambdaQueryWrapper<SalaryInfoVO>()
                        .eq(SalaryInfoVO::getSalaryMonth, month))
                .stream()
                .filter(r -> allowed.contains(r.getEmpId()))
                .map(SalaryInfoVO::getActualSalary)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<String> recentMonths(int n) {
        List<String> months = salaryInfoMapper.selectObjs(new LambdaQueryWrapper<SalaryInfoVO>()
                        .select(SalaryInfoVO::getSalaryMonth)
                        .groupBy(SalaryInfoVO::getSalaryMonth)
                        .orderByDesc(SalaryInfoVO::getSalaryMonth))
                .stream().map(Object::toString).collect(Collectors.toList());
        return months.size() > n ? months.subList(0, n) : months;
    }

    /** 查询薪资历史月份列表，供页面下拉 */
    public List<String> monthOptions() {
        return recentMonths(24);
    }

    private void validateMonth(String month) {
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw BizException.badRequest("月份格式应为 YYYY-MM，如 2026-09");
        }
    }
}
