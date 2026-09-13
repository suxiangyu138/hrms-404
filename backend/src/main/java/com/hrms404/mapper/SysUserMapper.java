package com.hrms404.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms404.entity.SysUser;
import com.hrms404.vo.LoginAuthVO;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper：登录账号查询
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 按登录名查用户（含已禁用，便于提示） */
    @Select("SELECT * FROM sys_user WHERE username = #{username} LIMIT 1")
    SysUser selectByUsername(String username);

    /**
     * 登录认证查询：账号 + 员工 + 部门 + 职位一次 JOIN 取齐。
     * 原实现为 4 次串行查询（sys_user → employee → department → position），
     * 合并后登录只需 1 次数据库往返。
     */
    @Select("""
            SELECT u.user_id, u.emp_id, u.username, u.password, u.role_code, u.enabled,
                   e.emp_name, e.dept_id, d.dept_name, p.position_name
            FROM sys_user u
                     LEFT JOIN employee   e ON u.emp_id = e.emp_id
                     LEFT JOIN department d ON e.dept_id = d.dept_id
                     LEFT JOIN position   p ON e.position_id = p.position_id
            WHERE u.username = #{username}
            LIMIT 1
            """)
    LoginAuthVO selectLoginByUsername(String username);

    /** 员工是否已绑定账号（sys_user.emp_id 为唯一键，最多 1 条） */
    @Select("SELECT COUNT(*) FROM sys_user WHERE emp_id = #{empId}")
    int countByEmpId(Long empId);
}
