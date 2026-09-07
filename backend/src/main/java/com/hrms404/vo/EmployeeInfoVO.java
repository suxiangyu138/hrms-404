package com.hrms404.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工完整信息（v_employee_full_info 只读视图，列表页免多表联查）
 */
@Data
@TableName("v_employee_full_info")
public class EmployeeInfoVO {

    @TableId
    private Long empId;

    private String empNo;
    private String empName;
    private String gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private String phone;
    private String email;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    /** 1 在职 0 离职 */
    private Integer status;
    private Long deptId;
    private String deptName;
    private Long positionId;
    private String positionName;
    private BigDecimal baseSalary;
}
