-- =====================================================================
-- HRMS-404 人事管理系统  视图脚本（3 个视图）
-- =====================================================================
USE hrms_db;

-- ---------- 1. 员工完整信息视图（含部门名、职位名，前端列表免联查） ----------
DROP VIEW IF EXISTS v_employee_full_info;
CREATE VIEW v_employee_full_info AS
SELECT e.emp_id,
       e.emp_no,
       e.emp_name,
       e.gender,
       e.birth_date,
       e.phone,
       e.email,
       e.hire_date,
       e.status,
       e.dept_id,
       d.dept_name,
       e.position_id,
       p.position_name,
       p.base_salary
FROM employee e
         JOIN department d ON e.dept_id = d.dept_id
         JOIN position p ON e.position_id = p.position_id;

-- ---------- 2. 考勤汇总视图（按员工×月份统计出勤情况） ----------
DROP VIEW IF EXISTS v_attendance_summary;
CREATE VIEW v_attendance_summary AS
SELECT a.emp_id,
       e.emp_name,
       d.dept_name,
       DATE_FORMAT(a.work_date, '%Y-%m') AS att_month,
       COUNT(*)                          AS work_days,
       SUM(a.status = 1)                 AS normal_cnt,
       SUM(a.status IN (2, 4))           AS late_cnt,
       SUM(a.status IN (3, 4))           AS early_cnt
FROM attendance a
         JOIN employee e ON a.emp_id = e.emp_id
         JOIN department d ON e.dept_id = d.dept_id
GROUP BY a.emp_id, e.emp_name, d.dept_name, DATE_FORMAT(a.work_date, '%Y-%m');

-- ---------- 3. 薪资历史视图（含员工姓名、部门，列表免联查） ----------
DROP VIEW IF EXISTS v_salary_history;
CREATE VIEW v_salary_history AS
SELECT s.salary_id,
       s.emp_id,
       e.emp_no,
       e.emp_name,
       d.dept_name,
       s.salary_month,
       s.base_salary,
       s.performance,
       s.deduction,
       s.actual_salary,
       s.created_at
FROM salary s
         JOIN employee e ON s.emp_id = e.emp_id
         JOIN department d ON e.dept_id = d.dept_id;
