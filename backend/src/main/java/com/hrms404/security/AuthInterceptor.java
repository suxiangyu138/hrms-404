package com.hrms404.security;

import com.hrms404.common.LoginSession;
import com.hrms404.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * 登录与页面级角色拦截器：
 * 1. 未登录：/api/* 返回 401 JSON；页面请求重定向到 /login
 * 2. 页面级 RBAC：按访问路径映射允许角色，越权返回 403
 * 3. 把会话用户注入 UserContext（ThreadLocal），请求结束清理
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_KEY = "LOGIN_USER";

    /** 页面路径 -> 允许角色（null 表示全部登录用户可访问） */
    private static final Map<String, Set<String>> PAGE_ROLES = Map.of(
            "/employees", Set.of("ADMIN", "HR", "MANAGER"),
            "/departments", Set.of("ADMIN", "HR"),
            "/positions", Set.of("ADMIN", "HR"),
            "/salaries", Set.of("ADMIN", "HR"),
            "/text2sql", Set.of("ADMIN", "HR", "MANAGER")
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession session = request.getSession(false);
        LoginSession loginUser = session == null ? null : (LoginSession) session.getAttribute(SESSION_KEY);

        // ---- 未登录 ----
        if (loginUser == null) {
            if (request.getRequestURI().startsWith("/api/")) {
                writeJson(response, 401, "401：登录已过期，请重新登录");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return false;
        }

        // ---- 登录成功：注入线程上下文 ----
        UserContext.set(loginUser);

        // ---- 页面级角色校验（数据级权限在 Service/Controller 内二次校验） ----
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            Set<String> allowed = PAGE_ROLES.getOrDefault(path, null);
            if (allowed != null && !allowed.contains(loginUser.getRoleCode())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "403：无权限访问该页面");
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeJson(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}";
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }
}
