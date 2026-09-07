package com.hrms404.security;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 密码工具：MD5(固定盐 + 密码) —— 与建库触发器/种子脚本保持一致
 * 盐固定为 "404n0tf0und"（呼应用户组名 404 Not Found）
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
