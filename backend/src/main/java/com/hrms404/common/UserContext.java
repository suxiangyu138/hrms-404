package com.hrms404.common;

/**
 * 请求级用户上下文：由 AuthInterceptor 在 preHandle 注入、afterCompletion 清理
 */
public final class UserContext {

    private static final ThreadLocal<LoginSession> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginSession user) {
        HOLDER.set(user);
    }

    public static LoginSession get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 当前用户角色，未登录返回 null */
    public static String role() {
        LoginSession u = HOLDER.get();
        return u == null ? null : u.getRoleCode();
    }

    /** 当前用户关联员工ID，未绑定员工（如 admin）返回 null */
    public static Long empId() {
        LoginSession u = HOLDER.get();
        return u == null ? null : u.getEmpId();
    }
}
