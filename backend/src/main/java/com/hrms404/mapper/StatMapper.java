package com.hrms404.mapper;

import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 统计查询 Mapper：仪表盘 + Text to SQL 表结构上下文
 */
public interface StatMapper {

    /** 系统整体统计（仪表盘/管理员） */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM employee WHERE status = 1)          AS active_emp,
              (SELECT COUNT(*) FROM employee WHERE status = 0)          AS leave_emp,
              (SELECT COUNT(*) FROM department)                          AS dept_count,
              (SELECT COUNT(*) FROM position)                            AS position_count,
              (SELECT COUNT(DISTINCT emp_id) FROM attendance
                WHERE work_date = CURDATE())                             AS today_attended,
              (SELECT IFNULL(SUM(actual_salary), 0) FROM salary
                WHERE salary_month = DATE_FORMAT(CURDATE(), '%Y-%m'))    AS month_payroll
            """)
    Map<String, Object> systemSummary();

    /** Text to SQL：读取当前库全部字段元数据（表名/列名/注释/类型） */
    @Select("""
            SELECT TABLE_NAME, COLUMN_NAME, COLUMN_COMMENT, DATA_TYPE, COLUMN_KEY
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
            ORDER BY TABLE_NAME, ORDINAL_POSITION
            """)
    List<Map<String, Object>> tableColumnMeta();
}
