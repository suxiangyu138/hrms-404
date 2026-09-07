package com.hrms404.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms404.entity.Salary;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 薪资 Mapper：CRUD + 调用月度薪资生成存储过程
 */
public interface SalaryMapper extends BaseMapper<Salary> {

    /** 调用 sp_generate_monthly_salary：返回本次实际生成的条数 */
    @Select("CALL sp_generate_monthly_salary(#{month})")
    List<Map<String, Object>> callGenerateMonthly(@Param("month") String month);
}
