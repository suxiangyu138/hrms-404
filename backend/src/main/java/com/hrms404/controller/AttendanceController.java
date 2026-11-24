package com.hrms404.controller;

import com.hrms404.common.Result;
import com.hrms404.entity.Attendance;
import com.hrms404.service.AttendanceService;
import com.hrms404.vo.AttendanceSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考勤接口：/api/attendance
 * - 打卡：每日一条，重复打卡 409（后端校验）
 * - 汇总：ADMIN/HR 全量、MANAGER 本部门子树、EMPLOYEE 仅本人明细
 */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /** 上班打卡 */
    @PostMapping("/clock-in")
    public Result<Attendance> clockIn() {
        Attendance a = attendanceService.clockIn();
        return Result.success("上班打卡成功：" + statusMsg(a.getStatus()), a);
    }

    /** 下班打卡 */
    @PostMapping("/clock-out")
    public Result<Attendance> clockOut() {
        Attendance a = attendanceService.clockOut();
        return Result.success("下班打卡成功：" + statusMsg(a.getStatus()), a);
    }

    /** 今日打卡状态（未打卡 data=null） */
    @GetMapping("/today")
    public Result<Attendance> today() {
        return Result.success(attendanceService.today());
    }

    /** 本人某月打卡明细（员工考勤页） */
    @GetMapping("/my")
    public Result<List<Attendance>> myMonth(@RequestParam String month) {
        return Result.success(attendanceService.myMonth(month));
    }

    /** 月度汇总（考勤管理页） */
    @GetMapping("/summary")
    public Result<List<AttendanceSummaryVO>> summary(@RequestParam String month) {
        return Result.success(attendanceService.summary(month));
    }

    private String statusMsg(int status) {
        return switch (status) {
            case 1 -> "状态：正常";
            case 2 -> "状态：迟到（9:00 后打卡）";
            case 3 -> "状态：早退（18:00 前下班）";
            case 4 -> "状态：迟到且早退";
            default -> "";
        };
    }
}
