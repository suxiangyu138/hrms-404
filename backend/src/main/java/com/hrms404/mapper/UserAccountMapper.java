package com.hrms404.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms404.vo.EmpCandidateVO;
import com.hrms404.vo.UserAccountVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 账号管理 Mapper（只读联查，写操作走 SysUserMapper）
 */
public interface UserAccountMapper {

    /**
     * 账号分页查询：sys_user LEFT JOIN employee/department，
     * 按角色优先级（管理员 → 人事 → 经理 → 员工）再按新建倒序排列。
     */
    @Select("""
            <script>
            SELECT u.user_id, u.emp_id, u.username, u.role_code, u.enabled, u.created_at,
                   e.emp_no, e.emp_name, d.dept_name
            FROM sys_user u
                     LEFT JOIN employee   e ON u.emp_id = e.emp_id
                     LEFT JOIN department d ON e.dept_id = d.dept_id
            <where>
                <if test="keyword != null and keyword != ''">
                    (u.username LIKE CONCAT('%', #{keyword}, '%')
                     OR e.emp_name LIKE CONCAT('%', #{keyword}, '%')
                     OR e.emp_no LIKE CONCAT('%', #{keyword}, '%'))
                </if>
                <if test="roleCode != null and roleCode != ''">
                    AND u.role_code = #{roleCode}
                </if>
                <if test="enabled != null">
                    AND u.enabled = #{enabled}
                </if>
            </where>
            ORDER BY FIELD(u.role_code, 'ADMIN', 'HR', 'MANAGER', 'EMPLOYEE'), u.user_id DESC
            </script>
            """)
    Page<UserAccountVO> selectAccountPage(Page<UserAccountVO> page,
                                          @Param("keyword") String keyword,
                                          @Param("roleCode") String roleCode,
                                          @Param("enabled") Integer enabled);

    /** 开通账号时的可选员工（在职优先展示，标记是否已有账号；最多 20 条） */
    @Select("""
            <script>
            SELECT e.emp_id, e.emp_no, e.emp_name, d.dept_name,
                   IF(u.user_id IS NULL, 0, 1) AS has_account
            FROM employee e
                     LEFT JOIN department d ON e.dept_id = d.dept_id
                     LEFT JOIN sys_user   u ON u.emp_id = e.emp_id
            WHERE e.status = 1
            <if test="keyword != null and keyword != ''">
                AND (e.emp_name LIKE CONCAT('%', #{keyword}, '%')
                     OR e.emp_no LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY has_account, e.emp_no
            LIMIT 20
            </script>
            """)
    List<EmpCandidateVO> searchCandidates(@Param("keyword") String keyword);
}
