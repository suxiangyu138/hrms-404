package com.hrms404.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms404.common.*;
import com.hrms404.entity.Attendance;
import com.hrms404.entity.Employee;
import com.hrms404.mapper.AttendanceMapper;
import com.hrms404.mapper.AttendanceSummaryMapper;
import com.hrms404.mapper.DepartmentMapper;
import com.hrms404.mapper.EmployeeMapper;
import com.hrms404.vo.AttendanceSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 考勤业务：
 * - 打卡：每日一条记录（重复打卡返回 409），上班 9:00 前为正常、下班 18:00 前为早退
 * - 月度汇总：ADMIN/HR 全量、MANAGER 本部门子树、EMPLOYEE 仅本人
 */
@Service
@RequiredArgsConstructor
public class AttendanceService {

    /** 上班迟到界限 09:00 */
    private static final LocalTime LATE_AFTER = LocalTime.of(9, 0);
    /** 下班早退界限 18:00 */
    private static final LocalTime EARLY_BEFORE = LocalTime.of(18, 0);

    private final AttendanceMapper attendanceMapper;
    private final AttendanceSummaryMapper attendanceSummaryMapper;
    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;

    /** 上班打卡：每日一卡，同日重复打卡提示冲突 */
    @Transactional
    public Attendance clockIn() {
        Long empId = requiredEmpId();
        LocalDate today = LocalDate.now();
        Attendance exist = todayRecord(empId, today);
        if (exist != null && exist.getCheckInTime() != null) {
            throw BizException.conflict("409：今日已打过上班卡（每日一卡），请勿重复打卡");
        }
        if (exist != null && exist.getCheckInTime() == null) {
            throw BizException.conflict("409：今日打卡记录异常，请联系 HR 处理");
        }
        Attendance a = new Attendance();
        a.setEmpId(empId);
        a.setWorkDate(today);
        a.setCheckInTime(LocalDateTime.now());
        a.setStatus(calcStatus(a.getCheckInTime(), null));
        attendanceMapper.insert(a);
        return a;
    }

    /** 下班打卡 */
    @Transactional
    public Attendance clockOut() {
        Long empId = requiredEmpId();
        LocalDate today = LocalDate.now();
        Attendance exist = todayRecord(empId, today);
        if (exist == null || exist.getCheckInTime() == null) {
            throw BizException.conflict("409：尚未打上班卡，无法打下班卡");
        }
        if (exist.getCheckOutTime() != null) {
            throw BizException.conflict("409：今日已打过下班卡，请勿重复打卡");
        }
        LocalDateTime now = LocalDateTime.now();
        exist.setCheckOutTime(now);
        exist.setStatus(calcStatus(exist.getCheckInTime(), now));
        attendanceMapper.updateById(exist);
        return exist;
    }

    /** 今日打卡状态（未打卡返回 null） */
    public Attendance today() {
        Long empId = UserContext.empId();
        if (empId == null) {
            return null;
        }
        return todayRecord(empId, LocalDate.now());
    }

    /** 本人某月打卡明细 */
    public List<Attendance> myMonth(String month) {
        Long empId = requiredEmpId();
        LocalDate start = parseMonth(month);
        return attendanceMapper.selectList(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getEmpId, empId)
                .between(Attendance::getWorkDate, start, start.plusMonths(1).minusDays(1))
                .orderByAsc(Attendance::getWorkDate));
    }

    /**
     * 月度考勤汇总：ADMIN/HR 全量；MANAGER 限定本部门子树；EMPLOYEE 禁止
     */
    public List<AttendanceSummaryVO> summary(String month) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR, Roles.MANAGER));
        LocalDate start = parseMonth(month);
        String ym = start.toString().substring(0, 7);

        List<AttendanceSummaryVO> rows = attendanceSummaryMapper.selectList(
                new LambdaQueryWrapper<AttendanceSummaryVO>()
                        .eq(AttendanceSummaryVO::getAttMonth, ym)
                        .orderByAsc(AttendanceSummaryVO::getEmpId));

        // MANAGER 数据范围：仅本部门子树的员工
        List<Long> scopedEmpIds = ScopeUtil.scopedEmpIds(departmentMapper, employeeMapper, UserContext.get());
        if (scopedEmpIds != null) {
            Set<Long> allowed = Set.copyOf(scopedEmpIds);
            return rows.stream().filter(r -> allowed.contains(r.getEmpId())).collect(Collectors.toList());
        }
        return rows;
    }

    private Attendance todayRecord(Long empId, LocalDate date) {
        return attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getEmpId, empId)
                .eq(Attendance::getWorkDate, date));
    }

    /** 计算考勤状态：1 正常 2 迟到 3 早退 4 迟到且早退 */
    private Integer calcStatus(LocalDateTime checkIn, LocalDateTime checkOut) {
        boolean late = checkIn != null && checkIn.toLocalTime().isAfter(LATE_AFTER);
        boolean early = checkOut != null && checkOut.toLocalTime().isBefore(EARLY_BEFORE);
        if (late && early) return 4;
        if (late) return 2;
        if (early) return 3;
        return 1;
    }

    private Long requiredEmpId() {
        Long empId = UserContext.empId();
        if (empId == null) {
            throw BizException.badRequest("当前账号未绑定员工（如 admin），无法打卡");
        }
        // 已离职员工禁止打卡
        Employee emp = employeeMapper.selectById(empId);
        if (emp == null || emp.getStatus() == null || emp.getStatus() != 1) {
            throw BizException.forbidden("403：账号关联员工已离职，无法打卡");
        }
        return empId;
    }

    private LocalDate parseMonth(String month) {
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw BizException.badRequest("月份格式应为 YYYY-MM，如 2026-09");
        }
        return LocalDate.parse(month + "-01");
    }
}
