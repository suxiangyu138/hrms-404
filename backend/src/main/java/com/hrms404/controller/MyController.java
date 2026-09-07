package com.hrms404.controller;

import com.hrms404.common.Result;
import com.hrms404.entity.Attendance;
import com.hrms404.service.AttendanceService;
import com.hrms404.service.EmployeeService;
import com.hrms404.service.SalaryService;
import com.hrms404.vo.EmployeeInfoVO;
import com.hrms404.vo.SalaryInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 个人中心接口：/api/my（当前登录用户本人的信息，EMP 员工主入口）
 */
@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MyController {

    private final EmployeeService employeeService;
    private final SalaryService salaryService;
    private final AttendanceService attendanceService;

    /** 个人基本信息（未绑定员工如 admin 返回 data=null） */
    @GetMapping("/info")
    public Result<EmployeeInfoVO> info() {
        return Result.success(employeeService.myInfo());
    }

    /** 我的薪资历史 */
    @GetMapping("/salaries")
    public Result<List<SalaryInfoVO>> salaries() {
        return Result.success(salaryService.mySalaries());
    }

    /** 我某月打卡明细 */
    @GetMapping("/attendance")
    public Result<List<Attendance>> attendance(@RequestParam String month) {
        return Result.success(attendanceService.myMonth(month));
    }

    /** 我的今日打卡状态 */
    @GetMapping("/attendance/today")
    public Result<Attendance> today() {
        return Result.success(attendanceService.today());
    }
}
