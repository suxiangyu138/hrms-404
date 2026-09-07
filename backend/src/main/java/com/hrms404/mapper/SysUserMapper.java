package com.hrms404.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms404.entity.SysUser;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper：登录账号查询
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 按登录名查用户（含已禁用，便于提示） */
    @Select("SELECT * FROM sys_user WHERE username = #{username} LIMIT 1")
    SysUser selectByUsername(String username);
}
