package com.hrms404.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms404.entity.Attendance;
import org.apache.ibatis.annotations.Select;

/**
 * 考勤 Mapper：CRUD + 今日考勤统计（每日一卡由 Service 校验唯一键）
 */
public interface AttendanceMapper extends BaseMapper<Attendance> {

    /** 今日已打卡的员工数（仪表盘用） */
    @Select("SELECT COUNT(DISTINCT emp_id) FROM attendance WHERE work_date = CURDATE()")
    Long countTodayAttendance();
}
