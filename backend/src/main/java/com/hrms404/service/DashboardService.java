package com.hrms404.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms404.common.Roles;
import com.hrms404.common.ScopeUtil;
import com.hrms404.common.UserContext;
import com.hrms404.entity.Attendance;
import com.hrms404.entity.Employee;
import com.hrms404.entity.Salary;
import com.hrms404.mapper.AttendanceMapper;
import com.hrms404.mapper.DepartmentMapper;
import com.hrms404.mapper.EmployeeMapper;
import com.hrms404.mapper.SalaryMapper;
import com.hrms404.mapper.StatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计：按角色返回个性化概览卡片
 * ADMIN/HR 全公司、MANAGER 本部门子树、EMPLOYEE 个人
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StatMapper statMapper;
    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;
    private final AttendanceMapper attendanceMapper;
    private final SalaryMapper salaryMapper;
    private final SalaryService salaryService;

    public List<Map<String, Object>> cards() {
        String role = UserContext.role();
        if (Roles.ADMIN.equals(role) || Roles.HR.equals(role)) {
            return companyCards();
        }
        if (Roles.MANAGER.equals(role)) {
            return managerCards();
        }
        return employeeCards();
    }

    private List<Map<String, Object>> companyCards() {
        Map<String, Object> s = statMapper.systemSummary();
        List<Map<String, Object>> cards = new ArrayList<>();
        cards.add(card("在职员工", s.get("active_emp"), "人", "text-bg-primary"));
        cards.add(card("部门数量", s.get("dept_count"), "个", "text-bg-success"));
        cards.add(card("今日已打卡", s.get("today_attended"), "人", "text-bg-warning"));
        cards.add(card("本月实发薪资", money(s.get("month_payroll")), "元", "text-bg-danger"));
        return cards;
    }

    private List<Map<String, Object>> managerCards() {
        var user = UserContext.get();
        List<Long> deptIds = ScopeUtil.scopedDeptIds(departmentMapper, user);
        List<Long> empIds = ScopeUtil.scopedEmpIds(departmentMapper, employeeMapper, user);
        int active = 0, today = 0;
        if (deptIds != null && !deptIds.isEmpty()) {
            active = Math.toIntExact(employeeMapper.selectCount(new LambdaQueryWrapper<Employee>()
                    .eq(Employee::getStatus, 1).in(Employee::getDeptId, deptIds)));
        }
        if (empIds != null && !empIds.isEmpty()) {
            today = Math.toIntExact(attendanceMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                    .eq(Attendance::getWorkDate, LocalDate.now()).in(Attendance::getEmpId, empIds)));
        }
        BigDecimal payroll = salaryService.payrollOf(empIds, currentMonth());
        List<Map<String, Object>> cards = new ArrayList<>();
        cards.add(card("本部门在职员工", active, "人", "text-bg-primary"));
        cards.add(card("今日部门打卡", today, "人", "text-bg-warning"));
        cards.add(card("部门本月薪资", payroll, "元", "text-bg-danger"));
        cards.add(card("所属部门", user.getDeptName(), "", "text-bg-success"));
        return cards;
    }

    private List<Map<String, Object>> employeeCards() {
        var user = UserContext.get();
        Long empId = user.getEmpId();
        List<Map<String, Object>> cards = new ArrayList<>();
        if (empId == null) {
            cards.add(card("账号类型", "管理员（未绑定员工）", "", "text-bg-dark"));
            return cards;
        }
        Attendance todayAtt = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getEmpId, empId)
                .eq(Attendance::getWorkDate, LocalDate.now()));
        String month = currentMonth();
        long monthDays = attendanceMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getEmpId, empId)
                .likeRight(Attendance::getWorkDate, month));
        Salary last = lastSalary(empId);

        cards.add(card("今日打卡", todayAtt == null ? "未打卡" : statusText(todayAtt.getStatus()), "",
                "text-bg-primary"));
        cards.add(card("本月打卡天数", monthDays, "天", "text-bg-success"));
        cards.add(card("最新实发工资", last == null ? "-" : last.getActualSalary(), "元", "text-bg-danger"));
        cards.add(card("所属部门", user.getDeptName(), "", "text-bg-warning"));
        return cards;
    }

    private Salary lastSalary(Long empId) {
        List<Salary> list = salaryMapper.selectList(new LambdaQueryWrapper<Salary>()
                .eq(Salary::getEmpId, empId)
                .orderByDesc(Salary::getSalaryMonth)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    private String currentMonth() {
        return LocalDate.now().toString().substring(0, 7);
    }

    private Map<String, Object> card(String label, Object value, String unit, String color) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("value", value);
        m.put("unit", unit);
        m.put("color", color);
        return m;
    }

    private Object money(Object v) {
        if (v instanceof BigDecimal bd) {
            return bd.stripTrailingZeros().toPlainString();
        }
        return v;
    }

    private String statusText(int status) {
        return switch (status) {
            case 1 -> "正常";
            case 2 -> "迟到";
            case 3 -> "早退";
            case 4 -> "迟到早退";
            default -> "未完成";
        };
    }
}
