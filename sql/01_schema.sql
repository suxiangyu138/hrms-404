-- =====================================================================
-- HRMS-404 人事管理系统数据库课程设计  建库建表脚本
-- 小组：404 Not Found
-- =====================================================================

CREATE DATABASE IF NOT EXISTS hrms_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hrms_db;

-- ---------- 按外键依赖顺序删除旧表 ----------
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS salary;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS position;
DROP TABLE IF EXISTS department;

-- ---------- 1. 部门表（自关联树形） ----------
CREATE TABLE department (
    dept_id           INT AUTO_INCREMENT COMMENT '部门编号',
    dept_name         VARCHAR(50)  NOT NULL COMMENT '部门名称',
    parent_id         INT          NULL     COMMENT '上级部门编号，NULL 表示顶级部门（自关联）',
    headcount_budget  INT          NULL     COMMENT '部门编制预算上限（人数），NULL 表示不限',
    PRIMARY KEY (dept_id),
    KEY idx_dept_parent (parent_id),
    CONSTRAINT fk_dept_parent FOREIGN KEY (parent_id) REFERENCES department (dept_id)
) ENGINE = InnoDB COMMENT = '部门表（上下级树形结构）';

-- ---------- 2. 职位表 ----------
CREATE TABLE position (
    position_id   INT AUTO_INCREMENT COMMENT '职位编号',
    position_name VARCHAR(50)   NOT NULL COMMENT '职位名称',
    dept_id       INT           NOT NULL COMMENT '所属部门',
    headcount     INT           NOT NULL DEFAULT 1 COMMENT '编制数',
    base_salary   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '岗位基本工资（元/月）',
    PRIMARY KEY (position_id),
    KEY idx_position_dept (dept_id),
    CONSTRAINT fk_position_dept FOREIGN KEY (dept_id) REFERENCES department (dept_id)
) ENGINE = InnoDB COMMENT = '职位表（部门 1:N）';

-- ---------- 3. 员工表 ----------
CREATE TABLE employee (
    emp_id      INT AUTO_INCREMENT COMMENT '员工内部编号',
    emp_no      VARCHAR(20)  NOT NULL COMMENT '工号',
    emp_name    VARCHAR(50)  NOT NULL COMMENT '姓名',
    gender      ENUM('男', '女') NOT NULL DEFAULT '男' COMMENT '性别',
    birth_date  DATE         NULL COMMENT '出生日期',
    phone       VARCHAR(20)  NULL COMMENT '电话',
    email       VARCHAR(100) NULL COMMENT '邮箱',
    hire_date   DATE         NOT NULL COMMENT '入职日期',
    dept_id     INT          NOT NULL COMMENT '所属部门',
    position_id INT          NOT NULL COMMENT '所属职位',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 在职，0 离职（逻辑删除）',
    PRIMARY KEY (emp_id),
    UNIQUE KEY uk_emp_no (emp_no),
    UNIQUE KEY uk_emp_phone (phone),
    KEY idx_emp_dept (dept_id),
    KEY idx_emp_position (position_id),
    CONSTRAINT fk_emp_dept FOREIGN KEY (dept_id) REFERENCES department (dept_id),
    CONSTRAINT fk_emp_position FOREIGN KEY (position_id) REFERENCES position (position_id)
) ENGINE = InnoDB COMMENT = '员工表';

-- ---------- 4. 考勤表 ----------
CREATE TABLE attendance (
    attendance_id  INT AUTO_INCREMENT COMMENT '考勤编号',
    emp_id         INT      NOT NULL COMMENT '员工编号',
    work_date      DATE     NOT NULL COMMENT '考勤日期',
    check_in_time  DATETIME NULL COMMENT '上班打卡时间',
    check_out_time DATETIME NULL COMMENT '下班打卡时间',
    status         TINYINT  NOT NULL DEFAULT 0 COMMENT '状态：1 正常 2 迟到 3 早退 4 迟到且早退 0 未完成',
    PRIMARY KEY (attendance_id),
    UNIQUE KEY uk_att_emp_date (emp_id, work_date),
    KEY idx_att_date (work_date),
    CONSTRAINT fk_att_emp FOREIGN KEY (emp_id) REFERENCES employee (emp_id)
) ENGINE = InnoDB COMMENT = '考勤表（每员工每日一条记录）';

-- ---------- 5. 薪资表 ----------
CREATE TABLE salary (
    salary_id     INT AUTO_INCREMENT COMMENT '薪资编号',
    emp_id        INT           NOT NULL COMMENT '员工编号',
    salary_month  VARCHAR(7)    NOT NULL COMMENT '发放月份 YYYY-MM',
    base_salary   DECIMAL(10,2) NOT NULL COMMENT '基本工资（取岗位工资）',
    performance   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '绩效奖金',
    deduction     DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '扣款（迟到早退等）',
    actual_salary DECIMAL(10,2) NULL COMMENT '实发工资（触发器自动计算）',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    PRIMARY KEY (salary_id),
    UNIQUE KEY uk_salary_emp_month (emp_id, salary_month),
    KEY idx_salary_month (salary_month),
    CONSTRAINT fk_salary_emp FOREIGN KEY (emp_id) REFERENCES employee (emp_id)
) ENGINE = InnoDB COMMENT = '薪资表（每员工每月一条记录）';

-- ---------- 6. 用户表（登录账号，与员工 1:1） ----------
CREATE TABLE sys_user (
    user_id    INT AUTO_INCREMENT COMMENT '用户编号',
    emp_id     INT          NULL COMMENT '关联员工编号（1:1，管理员可为 NULL）',
    username   VARCHAR(30)  NOT NULL COMMENT '登录名',
    password   CHAR(32)     NOT NULL COMMENT '密码：MD5(盐+密码)，盐固定为 404n0tf0und（呼应小组名）',
    role_code  VARCHAR(20)  NOT NULL DEFAULT 'EMPLOYEE' COMMENT '角色：ADMIN/HR/MANAGER/EMPLOYEE',
    enabled    TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用：1 启用 0 禁用（离职自动禁用）',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_emp (emp_id),
    UNIQUE KEY uk_user_name (username),
    CONSTRAINT fk_user_emp FOREIGN KEY (emp_id) REFERENCES employee (emp_id)
) ENGINE = InnoDB COMMENT = '用户表（RBAC：user-emp 1:1，角色属性直接存储）';
