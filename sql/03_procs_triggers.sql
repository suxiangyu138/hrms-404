-- =====================================================================
-- HRMS-404 人事管理系统  存储过程（3 个）+ 触发器（4 个，实际 5 个对象）
-- 注：trg_check_budget 因 MySQL 限制同一事件只允许一个触发器，
--     拆分为 INSERT / UPDATE 两个对象（名称后缀区分）
-- =====================================================================
USE hrms_db;

DELIMITER $$

-- ============ 存储过程 ============

-- ---------- 1. 统计指定部门及其全部子部门的在职员工数 ----------
DROP PROCEDURE IF EXISTS sp_count_employee_by_dept$$
CREATE PROCEDURE sp_count_employee_by_dept(IN p_dept_id INT)
COMMENT '统计指定部门及其子部门在职员工总数（递归 CTE）'
BEGIN
    WITH RECURSIVE dept_tree AS (
        SELECT dept_id FROM department WHERE dept_id = p_dept_id
        UNION ALL
        SELECT d.dept_id
        FROM department d
                 INNER JOIN dept_tree t ON d.parent_id = t.dept_id
    )
    SELECT COUNT(*) AS emp_count
    FROM employee e
             INNER JOIN dept_tree dt ON e.dept_id = dt.dept_id
    WHERE e.status = 1;
END$$

-- ---------- 2. 查询部门组织树（递归 CTE，含层级与全路径） ----------
DROP PROCEDURE IF EXISTS sp_get_org_tree$$
CREATE PROCEDURE sp_get_org_tree()
COMMENT '递归查询部门组织树，返回层级号与部门全路径'
BEGIN
    WITH RECURSIVE dept_tree AS (
        SELECT dept_id, dept_name, parent_id, headcount_budget,
               0 AS dept_level,
               CAST(dept_name AS CHAR(200)) AS dept_path
        FROM department
        WHERE parent_id IS NULL
        UNION ALL
        SELECT d.dept_id, d.dept_name, d.parent_id, d.headcount_budget,
               t.dept_level + 1,
               CONCAT(t.dept_path, ' / ', d.dept_name)
        FROM department d
                 INNER JOIN dept_tree t ON d.parent_id = t.dept_id
    )
    SELECT dept_id, dept_name, parent_id, headcount_budget, dept_level, dept_path
    FROM dept_tree
    ORDER BY dept_path;
END$$

-- ---------- 3. 按月批量生成薪资（幂等：已存在月份跳过） ----------
-- 绩效：当月无迟到早退且出勤>=1 天奖励 300；扣款：每次迟到或早退扣 20 元
DROP PROCEDURE IF EXISTS sp_generate_monthly_salary$$
CREATE PROCEDURE sp_generate_monthly_salary(IN p_month VARCHAR(7))
COMMENT '按考勤与职位月薪为在职员工生成当月薪资，重复调用不产生重复记录'
BEGIN
    INSERT INTO salary (emp_id, salary_month, base_salary, performance, deduction)
    SELECT e.emp_id,
           p_month,
           p.base_salary,
           CASE WHEN stats.late_cnt + stats.early_cnt > 0 THEN 0 ELSE 300 END AS performance,
           ROUND((stats.late_cnt + stats.early_cnt) * 20, 2)                  AS deduction
    FROM employee e
             JOIN position p ON e.position_id = p.position_id
             LEFT JOIN (SELECT a.emp_id,
                               SUM(a.status IN (2, 4)) AS late_cnt,
                               SUM(a.status IN (3, 4)) AS early_cnt
                        FROM attendance a
                        WHERE DATE_FORMAT(a.work_date, '%Y-%m') = p_month
                        GROUP BY a.emp_id) stats ON stats.emp_id = e.emp_id
    WHERE e.status = 1
      AND NOT EXISTS (SELECT 1 FROM salary s WHERE s.emp_id = e.emp_id AND s.salary_month = p_month);
    SELECT ROW_COUNT() AS generated_count;
END$$

-- ============ 触发器 ============

-- ---------- 1. 新增员工后自动创建默认用户账号（登录名=工号，默认密码 123456） ----------
DROP TRIGGER IF EXISTS trg_after_emp_insert$$
CREATE TRIGGER trg_after_emp_insert
    AFTER INSERT
    ON employee
    FOR EACH ROW
BEGIN
    INSERT INTO sys_user (emp_id, username, password, role_code, enabled)
    VALUES (NEW.emp_id, NEW.emp_no, MD5(CONCAT('404n0tf0und', '123456')), 'EMPLOYEE', 1);
END$$

-- ---------- 2. 员工离职自动禁用账号 / 复职自动启用 ----------
DROP TRIGGER IF EXISTS trg_after_emp_leave$$
CREATE TRIGGER trg_after_emp_leave
    AFTER UPDATE
    ON employee
    FOR EACH ROW
BEGIN
    IF OLD.status = 1 AND NEW.status = 0 THEN
        UPDATE sys_user SET enabled = 0 WHERE emp_id = NEW.emp_id;
    ELSEIF OLD.status = 0 AND NEW.status = 1 THEN
        UPDATE sys_user SET enabled = 1 WHERE emp_id = NEW.emp_id;
    END IF;
END$$

-- ---------- 3. 插入薪资记录前自动计算实发工资（基本 + 绩效 - 扣款） ----------
DROP TRIGGER IF EXISTS trg_before_salary_insert$$
CREATE TRIGGER trg_before_salary_insert
    BEFORE INSERT
    ON salary
    FOR EACH ROW
BEGIN
    SET NEW.actual_salary =
            IFNULL(NEW.base_salary, 0) + IFNULL(NEW.performance, 0) - IFNULL(NEW.deduction, 0);
END$$

-- ---------- 4. 编制预算检查：职位编制数之和不得超过部门编制预算上限（INSERT） ----------
DROP TRIGGER IF EXISTS trg_check_budget_insert$$
CREATE TRIGGER trg_check_budget_insert
    BEFORE INSERT
    ON position
    FOR EACH ROW
BEGIN
    DECLARE v_total   INT;
    DECLARE v_budget  INT;
    SELECT headcount_budget INTO v_budget FROM department WHERE dept_id = NEW.dept_id;
    IF v_budget IS NOT NULL THEN
        SELECT IFNULL(SUM(headcount), 0) INTO v_total
        FROM position WHERE dept_id = NEW.dept_id;
        IF v_total + NEW.headcount > v_budget THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '超出部门编制预算上限，新增职位被拒绝';
        END IF;
    END IF;
END$$

-- ---------- 5. 编制预算检查（UPDATE） ----------
DROP TRIGGER IF EXISTS trg_check_budget_update$$
CREATE TRIGGER trg_check_budget_update
    BEFORE UPDATE
    ON position
    FOR EACH ROW
BEGIN
    DECLARE v_total   INT;
    DECLARE v_budget  INT;
    SELECT headcount_budget INTO v_budget FROM department WHERE dept_id = NEW.dept_id;
    IF v_budget IS NOT NULL THEN
        SELECT IFNULL(SUM(headcount), 0) INTO v_total
        FROM position
        WHERE dept_id = NEW.dept_id AND position_id <> OLD.position_id;
        IF v_total + NEW.headcount > v_budget THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '超出部门编制预算上限，修改被拒绝';
        END IF;
    END IF;
END$$

DELIMITER ;
