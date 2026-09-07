-- =====================================================================
-- HRMS-404 人事管理系统  种子数据脚本
-- 需依次执行 01_schema.sql → 02_views.sql → 03_procs_triggers.sql → 04_seed.sql
-- 默认登录账号见文末注释
-- =====================================================================
USE hrms_db;

-- ---------- 部门（3 级树：总公司 → 部门 → 小组） ----------
INSERT INTO department (dept_id, dept_name, parent_id, headcount_budget) VALUES
(1, '总公司',       NULL, 200),
(2, '研发部',       1,    60),
(3, '市场部',       1,    40),
(4, '人事部',       1,    20),
(5, '财务部',       1,    15),
(6, '销售部',       1,    40),
(7, '研发一组',     2,    25),
(8, '研发二组',     2,    25);

-- ---------- 职位 ----------
INSERT INTO position (position_id, position_name, dept_id, headcount, base_salary) VALUES
(1,  '研发经理',   2,  2,  18000.00),
(2,  '高级工程师', 7,  8,  12000.00),
(3,  '工程师',     7,  15, 9000.00),
(4,  '高级工程师', 8,  8,  12000.00),
(5,  '工程师',     8,  15, 9000.00),
(6,  '市场专员',   3,  30, 7000.00),
(7,  '人事专员',   4,  12, 6500.00),
(8,  '会计',       5,  8,  7500.00),
(9,  '销售经理',   6,  1,  14000.00),
(10, '销售专员',   6,  25, 6000.00);

-- ---------- 员工（15 人：14 在职 + 1 演示离职） ----------
INSERT INTO employee (emp_id, emp_no, emp_name, gender, birth_date, phone, email, hire_date, dept_id, position_id, status) VALUES
(1,  'E001', '张伟', '男', '1995-03-12', '13800000001', 'zhangwei@hrms404.com', '2020-06-15', 7, 2, 1),
(2,  'E002', '李娜', '女', '1998-07-21', '13800000002', 'lina@hrms404.com',    '2021-09-01', 7, 3, 1),
(3,  'E003', '王强', '男', '1996-11-03', '13800000003', 'wangqiang@hrms404.com','2020-08-10', 7, 3, 1),
(4,  'E004', '赵敏', '女', '1994-05-18', '13800000004', 'zhaomin@hrms404.com', '2019-04-22', 8, 4, 1),
(5,  'E005', '刘洋', '男', '1999-01-29', '13800000005', 'liuyang@hrms404.com', '2022-03-14', 8, 5, 1),
(6,  'E006', '陈静', '女', '1997-09-08', '13800000006', 'chenjing@hrms404.com','2021-07-19', 3, 6, 1),
(7,  'E007', '杨帆', '男', '1995-12-25', '13800000007', 'yangfan@hrms404.com', '2020-10-12', 3, 6, 1),
(8,  'E008', '黄丽', '女', '1993-02-14', '13800000008', 'huangli@hrms404.com', '2018-09-03', 4, 7, 1),
(9,  'E009', '周杰', '男', '1996-08-30', '13800000009', 'zhoujie@hrms404.com', '2020-05-06', 4, 7, 1),
(10, 'E010', '吴倩', '女', '1997-04-17', '13800000010', 'wuqian@hrms404.com', '2021-11-01', 5, 8, 1),
(11, 'E011', '郑浩', '男', '1992-10-09', '13800000011', 'zhenghao@hrms404.com','2017-03-20', 5, 8, 1),
(12, 'E012', '孙琳', '女', '1999-06-23', '13800000012', 'sunlin@hrms404.com', '2022-08-08', 6, 10, 1),
(13, 'E013', '马超', '男', '1998-02-27', '13800000013', 'machao@hrms404.com', '2021-02-22', 6, 10, 1),
(14, 'E014', '林芳', '女', '1991-12-01', '13800000014', 'linfang@hrms404.com', '2016-08-15', 6, 9, 1),
(15, 'E015', '陈晨', '男', '1996-05-05', '13800000015', 'chenchen@hrms404.com','2020-01-13', 4, 7, 1);
-- E015 通过 UPDATE 走"离职触发器"路径（自动禁用其账号），与真实业务一致
UPDATE employee SET status = 0 WHERE emp_id = 15;

-- ---------- 考勤（8 月、9 月工作日自动生成，含迟到/早退示例） ----------
INSERT INTO attendance (emp_id, work_date, check_in_time, check_out_time, status)
SELECT e.emp_id,
       wk.work_date,
       TIMESTAMP(wk.work_date, '08:55:00'),
       TIMESTAMP(wk.work_date, '18:05:00'),
       1
FROM employee e
         JOIN (WITH RECURSIVE weekdays AS (
                 SELECT '2026-08-03' AS work_date
                 UNION ALL
                 SELECT DATE_ADD(work_date, INTERVAL 1 DAY) FROM weekdays WHERE work_date < '2026-08-31')
               SELECT work_date FROM weekdays WHERE WEEKDAY(work_date) < 5) wk
              ON e.status = 1
ORDER BY e.emp_id, wk.work_date;

INSERT INTO attendance (emp_id, work_date, check_in_time, check_out_time, status)
SELECT e.emp_id,
       wk.work_date,
       TIMESTAMP(wk.work_date, '08:55:00'),
       TIMESTAMP(wk.work_date, '18:05:00'),
       1
FROM employee e
         JOIN (WITH RECURSIVE weekdays AS (
                 SELECT '2026-09-01' AS work_date
                 UNION ALL
                 SELECT DATE_ADD(work_date, INTERVAL 1 DAY) FROM weekdays WHERE work_date < '2026-09-30')
               SELECT work_date FROM weekdays WHERE WEEKDAY(work_date) < 5) wk
              ON e.status = 1 AND e.emp_id <> 15
WHERE wk.work_date <= '2026-09-04';   -- 本月数据到上周为止，今天起可演示"打卡"

-- 迟到示例：王强 9-02 / 陈静 8-05
UPDATE attendance SET check_in_time = '2026-09-02 09:15:00', status = 2 WHERE emp_id = 3  AND work_date = '2026-09-02';
UPDATE attendance SET check_in_time = '2026-08-05 09:22:00', status = 2 WHERE emp_id = 6  AND work_date = '2026-08-05';
-- 早退示例：孙琳 9-03 / 马超 8-06
UPDATE attendance SET check_out_time = '2026-09-03 17:20:00', status = 3 WHERE emp_id = 12 AND work_date = '2026-09-03';
UPDATE attendance SET check_out_time = '2026-08-06 17:10:00', status = 3 WHERE emp_id = 13 AND work_date = '2026-08-06';
-- 迟到+早退示例：刘洋 9-04 / 郑浩 8-10
UPDATE attendance SET check_in_time = '2026-09-04 09:18:00', check_out_time = '2026-09-04 17:10:00', status = 4
WHERE emp_id = 5 AND work_date = '2026-09-04';
UPDATE attendance SET check_in_time = '2026-08-10 09:12:00', check_out_time = '2026-08-10 17:05:00', status = 4
WHERE emp_id = 11 AND work_date = '2026-08-10';

-- ---------- 用户（触发器已为全部员工按工号建号：E001~E015，密码 123456） ----------
-- 密码统一：123456，存储 MD5('404n0tf0und123456') = 4a52b964ffd064171ff2c44773bd13a2
-- 1) 管理员账号（不绑定员工）
INSERT INTO sys_user (emp_id, username, password, role_code) VALUES
(NULL, 'admin', '4a52b964ffd064171ff2c44773bd13a2', 'ADMIN');
-- 2) 特殊角色账号：直接把触发器生成的员工账号改名为业务账号（演示离职禁用仍生效）
UPDATE sys_user SET username = 'hr01',      role_code = 'HR'       WHERE emp_id = 8;
UPDATE sys_user SET username = 'manager01', role_code = 'MANAGER'  WHERE emp_id = 14;

-- ---------- 薪资（调用存储过程生成 8 月历史 + 9 月预览） ----------
CALL sp_generate_monthly_salary('2026-08');
CALL sp_generate_monthly_salary('2026-09');

-- =====================================================================
-- 演示账号（密码均为 123456）：
--   admin     系统管理员（所有功能）
--   hr01      人事专员黄丽（员工/部门/职位/考勤/薪资管理）
--   manager01 销售经理林芳（仅查看销售部员工与数据）
--   E008/E001... 任意在职员工的工号即登录名（普通员工，我的/打卡）
-- =====================================================================
