package com.hrms404.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 员工表（离职为逻辑删除：status 置 0，触发器自动禁用关联账号）
 */
@Data
@TableName("employee")
public class Employee {

    @TableId(type = IdType.AUTO)
    private Long empId;

    /** 工号 */
    private String empNo;

    /** 姓名 */
    private String empName;

    /** 性别 男/女 */
    private String gender;

    /** 出生日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    /** 电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 入职日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    /** 所属部门 */
    private Long deptId;

    /** 所属职位 */
    private Long positionId;

    /** 1 在职 0 离职 */
    private Integer status;
}
