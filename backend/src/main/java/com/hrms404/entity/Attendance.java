package com.hrms404.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤表（每员工每日一条记录）
 * status：1 正常 2 迟到 3 早退 4 迟到且早退 0 未完成
 */
@Data
@TableName("attendance")
public class Attendance {

    @TableId(type = IdType.AUTO)
    private Long attendanceId;

    private Long empId;

    /** 考勤日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDate;

    /** 上班打卡时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkInTime;

    /** 下班打卡时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkOutTime;

    /** 状态：1 正常 2 迟到 3 早退 4 迟到且早退 0 未完成 */
    private Integer status;
}
