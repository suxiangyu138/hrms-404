package com.hrms404.security;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 密码工具：MD5(固定盐 + 密码) —— 与建库触发器/种子脚本保持一致
 * 盐固定为 "404n0tf0und"（呼应用户组名 404 Not Found）
 *
 * <p><b>仅用于课程演示，请勿用于生产环境。</b>盐写死在源码里且全局唯一，
 * 彩虹表与同盐碰撞都挡不住，MD5 本身也不适合做口令哈希。
 * 生产环境应改用 BCrypt / Argon2 这类带随机盐与可调工作因子的算法。
 * 保留此实现是为了让「应用层加密」与「数据库触发器加密」用同一套算法，
 * 便于在 SQL 脚本里直接构造可登录的种子账号。
 */
public final class PasswordUtil {

    public static final String SALT = "404n0tf0und";

    private PasswordUtil() {
    }

    public static String encrypt(String rawPassword) {
        String text = SALT + (rawPassword == null ? "" : rawPassword);
        return DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean matches(String rawPassword, String storedHash) {
        if (storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        return storedHash.equalsIgnoreCase(encrypt(rawPassword));
    }
}
