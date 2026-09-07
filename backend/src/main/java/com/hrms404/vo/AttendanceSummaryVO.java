package com.hrms404.vo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 考勤月度汇总（v_attendance_summary 只读视图）
 */
@Data
@TableName("v_attendance_summary")
public class AttendanceSummaryVO {

    private Long empId;
    private String empName;
    private String deptName;
    private String attMonth;
    private Integer workDays;
    private Integer normalCnt;
    private Integer lateCnt;
    private Integer earlyCnt;
}
