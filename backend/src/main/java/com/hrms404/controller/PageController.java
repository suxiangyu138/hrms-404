package com.hrms404.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面路由（Thymeleaf 服务端渲染）
 * 登录校验与页面级角色控制在 AuthInterceptor
 */
@Controller
public class PageController {

    /** 登录页（拦截器放行路径） */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** 仪表盘 */
    @GetMapping("/")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/employees")
    public String employees() {
        return "employees";
    }

    @GetMapping("/departments")
    public String departments() {
        return "departments";
    }

    @GetMapping("/positions")
    public String positions() {
        return "positions";
    }

    @GetMapping("/attendance")
    public String attendance() {
        return "attendance";
    }

    @GetMapping("/salaries")
    public String salaries() {
        return "salaries";
    }

    @GetMapping("/text2sql")
    public String text2sql() {
        return "text2sql";
    }

    /** 我的中心（普通员工个人数据） */
    @GetMapping("/my")
    public String my() {
        return "my";
    }
}
