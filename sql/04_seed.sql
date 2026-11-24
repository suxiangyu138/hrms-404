-- =====================================================================
-- HRMS-404 人事管理系统  种子数据脚本（中型公司规模：1000+ 员工）
-- 需依次执行 01_schema.sql → 02_views.sql → 03_procs_triggers.sql → 04_seed.sql
-- 数据规模：25 个部门（3 级树）· 52 个职位 · 1002 名员工（约 21 人离职）
--           考勤约 2.4 万条（8 月 21 个工作日 + 9 月 4 个工作日）
--           薪资由存储过程按 8 月/9 月批量生成
-- 默认登录账号见文末注释
-- =====================================================================
USE hrms_db;
SET SESSION cte_max_recursion_depth = 5000;

-- =====================================================================
-- 一、部门（3 级组织树：总公司 → 中心/部门 → 小组）
-- =====================================================================
INSERT INTO department (dept_id, dept_name, parent_id, headcount_budget) VALUES
(1,  '总公司',     NULL, 1200),
(2,  '研发中心',   1,    380),
(3,  '市场部',     1,    60),
(4,  '人事部',     1,    40),
(5,  '财务部',     1,    35),
(6,  '销售中心',   1,    210),
(7,  '研发一组',   2,    110),
(8,  '研发二组',   2,    110),
(9,  '研发三组',   2,    110),
(10, '架构组',     2,    40),
(11, '产品部',     1,    90),
(12, '销售一部',   6,    75),
(13, '销售二部',   6,    75),
(14, '渠道部',     6,    50),
(15, '客服一部',   1,    90),
(16, '客服二部',   1,    90),
(17, '行政部',     1,    30),
(18, '法务部',     1,    15),
(19, 'IT运维部',   1,    40),
(20, '质量保障部', 1,    45),
(21, '数据部',     1,    30),
(22, '大客户部',   1,    40),
(23, '海外业务部', 1,    35),
(24, '仓储部',     1,    45),
(25, '电商部',     1,    55);

-- =====================================================================
-- 二、职位（52 个，编制数总和受各部门编制预算约束，全部通过触发器校验）
-- =====================================================================
INSERT INTO position (position_id, position_name, dept_id, headcount, base_salary) VALUES
(1,  '研发经理',    7,  3,  22000.00),
(2,  '高级工程师',  7,  35, 16000.00),
(3,  '工程师',      7,  50, 11000.00),
(4,  '测试工程师',  7,  18, 9500.00),
(5,  '高级工程师',  8,  35, 16000.00),
(6,  '工程师',      8,  50, 11000.00),
(7,  '测试工程师',  8,  15, 9500.00),
(8,  '研发经理',    9,  3,  22000.00),
(9,  '高级工程师',  9,  40, 16000.00),
(10, '工程师',      9,  48, 11000.00),
(11, '架构师',      10, 8,  30000.00),
(12, '高级工程师',  10, 18, 16000.00),
(13, '产品经理',    11, 12, 18000.00),
(14, '产品专员',    11, 40, 10000.00),
(15, 'UI设计师',    11, 15, 11000.00),
(16, '市场经理',    3,  4,  15000.00),
(17, '市场专员',    3,  40, 8000.00),
(18, '品牌专员',    3,  10, 9000.00),
(19, '销售经理',    12, 4,  18000.00),
(20, '销售专员',    12, 60, 7000.00),
(21, '销售经理',    13, 4,  18000.00),
(22, '销售专员',    13, 60, 7000.00),
(23, '渠道经理',    14, 3,  16000.00),
(24, '渠道专员',    14, 40, 7500.00),
(25, '客服主管',    15, 4,  9000.00),
(26, '客服专员',    15, 80, 6000.00),
(27, '客服主管',    16, 4,  9000.00),
(28, '客服专员',    16, 80, 6000.00),
(29, '人事经理',    4,  3,  16000.00),
(30, '人事专员',    4,  25, 8500.00),
(31, '招聘专员',    4,  8,  8000.00),
(32, '财务经理',    5,  2,  17000.00),
(33, '会计',        5,  15, 9000.00),
(34, '出纳',        5,  8,  7000.00),
(35, '行政经理',    17, 2,  14000.00),
(36, '行政专员',    17, 18, 6500.00),
(37, '法务专员',    18, 8,  10000.00),
(38, '运维经理',    19, 2,  16000.00),
(39, '运维工程师',  19, 20, 10000.00),
(40, '网络工程师',  19, 8,  9500.00),
(41, '测试经理',    20, 2,  17000.00),
(42, '测试工程师',  20, 30, 9500.00),
(43, '数据工程师',  21, 15, 14000.00),
(44, '数据分析师',  21, 8,  12000.00),
(45, '大客户经理',  22, 4,  18000.00),
(46, '大客户专员',  22, 30, 9000.00),
(47, '海外业务经理', 23, 3, 18000.00),
(48, '海外业务专员', 23, 25, 10000.00),
(49, '仓储主管',    24, 3,  11000.00),
(50, '仓储专员',    24, 35, 6500.00),
(51, '电商运营专员', 25, 40, 9000.00),
(52, '电商美工',    25, 8,  8000.00);

-- =====================================================================
-- 三、员工（1002 人，递归 CTE 批量生成，入职触发器自动按工号创建账号）
--     按"职位槽位计划"分配：每个职位的目标人数，编号按职位顺序连续
-- =====================================================================
DROP TEMPORARY TABLE IF EXISTS slot_plan;
CREATE TEMPORARY TABLE slot_plan (position_id INT PRIMARY KEY, cnt INT);
INSERT INTO slot_plan VALUES
(1,3),(2,32),(3,45),(4,15),
(5,32),(6,45),(7,13),
(8,3),(9,35),(10,42),
(11,7),(12,15),
(13,10),(14,34),(15,12),
(16,4),(17,35),(18,8),
(19,4),(20,55),
(21,4),(22,55),
(23,3),(24,36),
(25,4),(26,80),
(27,4),(28,80),
(29,3),(30,22),(31,7),
(32,2),(33,13),(34,7),
(35,2),(36,16),
(37,7),
(38,2),(39,17),(40,7),
(41,2),(42,26),
(43,13),(44,7),
(45,4),(46,27),
(47,3),(48,22),
(49,3),(50,32),
(51,36),(52,7);

INSERT INTO employee (emp_no, emp_name, gender, birth_date, phone, email, hire_date, dept_id, position_id, status)
WITH RECURSIVE nums AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM nums WHERE n < 1002
),
slots AS (
    SELECT sp.position_id, p.dept_id,
           COALESCE(SUM(sp.cnt) OVER (ORDER BY sp.position_id ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) + 1 AS start_n,
           SUM(sp.cnt) OVER (ORDER BY sp.position_id) AS end_n
    FROM slot_plan sp
             JOIN position p ON p.position_id = sp.position_id
)
SELECT CONCAT('E', LPAD(n.n, 4, '0')),
       CONCAT(ELT(1 + FLOOR(RAND() * 20), '张','李','王','刘','陈','杨','赵','黄','周','吴','徐','孙','胡','朱','高','林','何','郭','马','罗'),
              ELT(1 + FLOOR(RAND() * 30), '伟','娜','强','敏','洋','静','帆','丽','杰','倩','磊','婷','军','雪','涛','娟','鹏','慧','晨','悦','宇','欣','浩','琳','轩','梅','博','丹','俊','颖')),
       IF(RAND() < 0.47, '男', '女'),
       DATE_ADD('1975-01-01', INTERVAL FLOOR(RAND() * 10000) DAY),
       CONCAT('137', LPAD(n.n, 8, '0')),
       CONCAT('emp', n.n, '@hrms404.com'),
       DATE_ADD('2005-01-01', INTERVAL FLOOR(RAND() * 7800) DAY),
       s.dept_id,
       s.position_id,
       1
FROM nums n
         JOIN slots s ON n.n BETWEEN s.start_n AND s.end_n;

-- 离职约 2%（21 人）：走 UPDATE 离职路径，触发器自动禁用其登录账号
UPDATE employee SET status = 0 WHERE emp_id % 47 = 0;

-- =====================================================================
-- 四、用户（触发器已为 1002 名员工按工号建号；以下设置演示特权账号）
--     密码统一：123456，存储 MD5('404n0tf0und123456') = 4a52b964ffd064171ff2c44773bd13a2
-- =====================================================================
INSERT INTO sys_user (emp_id, username, password, role_code) VALUES
(NULL, 'admin', '4a52b964ffd064171ff2c44773bd13a2', 'ADMIN');

-- 特殊角色账号：把触发器生成的员工账号改名为业务账号（离职禁用逻辑仍生效）
UPDATE sys_user SET username = 'hr01',      role_code = 'HR'      WHERE emp_id = (SELECT MIN(emp_id) FROM employee WHERE position_id = 30);
UPDATE sys_user SET username = 'manager01', role_code = 'MANAGER' WHERE emp_id = (SELECT MIN(emp_id) FROM employee WHERE position_id = 19);

-- =====================================================================
-- 五、考勤（在职员工）：8 月 21 个工作日 + 9 月 4 个工作日
--     状态随机分布：约 94% 正常、6% 迟到/早退/迟到且早退；
--     时间与状态联动生成，保证逻辑一致
-- =====================================================================
INSERT INTO attendance (emp_id, work_date, check_in_time, check_out_time, status)
WITH RECURSIVE weekdays AS (
    SELECT '2026-08-03' AS work_date
    UNION ALL
    SELECT DATE_ADD(work_date, INTERVAL 1 DAY) FROM weekdays WHERE work_date < '2026-08-31'
)
SELECT t.emp_id, t.work_date,
       IF(t.st IN (2, 4),
          TIMESTAMP(t.work_date, SEC_TO_TIME(32400 + FLOOR(RAND() * 2400))),   -- 9:00~9:40 迟到
          TIMESTAMP(t.work_date, SEC_TO_TIME(30000 + FLOOR(RAND() * 2280)))),  -- 8:20~8:58 正常
       IF(t.st IN (3, 4),
          TIMESTAMP(t.work_date, SEC_TO_TIME(60600 + FLOOR(RAND() * 4140))),   -- 16:50~17:59 早退
          TIMESTAMP(t.work_date, SEC_TO_TIME(63000 + FLOOR(RAND() * 4200)))),  -- 17:30~18:40 正常
       t.st
FROM (
    SELECT e.emp_id, wk.work_date,
           IF(RAND() < 0.94, 1, ELT(1 + FLOOR(RAND() * 3), 2, 3, 4)) AS st
    FROM employee e
             JOIN (SELECT work_date FROM weekdays WHERE WEEKDAY(work_date) < 5) wk
    WHERE e.status = 1
) t;

INSERT INTO attendance (emp_id, work_date, check_in_time, check_out_time, status)
WITH RECURSIVE weekdays AS (
    SELECT '2026-09-01' AS work_date
    UNION ALL
    SELECT DATE_ADD(work_date, INTERVAL 1 DAY) FROM weekdays WHERE work_date < '2026-09-04'
)
SELECT t.emp_id, t.work_date,
       IF(t.st IN (2, 4),
          TIMESTAMP(t.work_date, SEC_TO_TIME(32400 + FLOOR(RAND() * 2400))),
          TIMESTAMP(t.work_date, SEC_TO_TIME(30000 + FLOOR(RAND() * 2280)))),
       IF(t.st IN (3, 4),
          TIMESTAMP(t.work_date, SEC_TO_TIME(60600 + FLOOR(RAND() * 4140))),
          TIMESTAMP(t.work_date, SEC_TO_TIME(63000 + FLOOR(RAND() * 4200)))),
       t.st
FROM (
    SELECT e.emp_id, wk.work_date,
           IF(RAND() < 0.94, 1, ELT(1 + FLOOR(RAND() * 3), 2, 3, 4)) AS st
    FROM employee e
             JOIN (SELECT work_date FROM weekdays WHERE WEEKDAY(work_date) < 5) wk
    WHERE e.status = 1
) t;

-- =====================================================================
-- 六、薪资：调用存储过程为在职员工批量生成 8 月/9 月薪资（幂等，触发器算实发）
-- =====================================================================
CALL sp_generate_monthly_salary('2026-08');
CALL sp_generate_monthly_salary('2026-09');

-- =====================================================================
-- 演示账号（密码均为 123456）：
--   admin     系统管理员（所有功能）
--   hr01      人事专员（绑定人事部某人事专员员工）
--   manager01 销售经理（绑定销售一部某销售经理，数据范围=销售一部及子部门）
--   E0001~E1002  任意在职员工的工号即登录名（普通员工：打卡/我的中心）
--   （离职员工账号已被触发器自动禁用，可演示登录被拒绝）
-- =====================================================================
