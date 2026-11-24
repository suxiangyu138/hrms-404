package com.hrms404.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms404.entity.Employee;
import org.apache.ibatis.annotations.Select;

/**
 * 员工 Mapper：标准 CRUD（离职=逻辑删除 update status，触发器联动禁用账号）
 */
public interface EmployeeMapper extends BaseMapper<Employee> {

    /** 取当前最大工号序号（E001 格式）用于自动生成工号 */
    @Select("SELECT MAX(CAST(SUBSTRING(emp_no, 2) AS UNSIGNED)) FROM employee WHERE emp_no LIKE 'E%'")
    Integer maxEmpSeq();
}
