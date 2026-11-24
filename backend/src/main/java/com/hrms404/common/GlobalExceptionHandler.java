package com.hrms404.common;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器：把各类异常统一转换为 Result JSON，避免异常堆栈直达前端
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常（携带自定义错误码） */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常[{}]：{}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 访问不存在的路由/静态资源：页面请求交给 Boot 错误视图渲染品牌 404 页，
     * /api 请求返回标准 JSON 404
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResource(NoResourceFoundException e, HttpServletResponse response) throws IOException {
        log.info("页面/资源不存在：{}", e.getResourcePath());
        String path = e.getResourcePath();
        if (path != null && path.startsWith("api")) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":404,\"message\":\"404：请求的接口不存在\",\"data\":null}");
        } else {
            // 交给 /error → templates/error/404.html（小组品牌 404 页）
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "404 NOT FOUND");
        }
    }

    /** 请求参数缺失 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error(400, "缺少请求参数：" + e.getParameterName());
    }

    /** 唯一键冲突（工号/手机号/账号重复等） */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicate(DuplicateKeyException e) {
        log.warn("唯一键冲突：{}", e.getMessage());
        return Result.error(409, "记录已存在：工号/登录名/手机号等唯一字段重复，请检查后重试");
    }

    /** 数据库完整性约束（触发器 SIGNAL、外键约束等） */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<Void> handleIntegrity(SQLIntegrityConstraintViolationException e) {
        log.warn("数据库约束冲突：{}", e.getMessage());
        return Result.error(409, friendlyDbMessage(e.getMessage()));
    }

    /** MyBatis 包装后的数据完整性异常 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("数据完整性异常：{}", e.getMessage());
        Throwable root = e.getMostSpecificCause();
        if (root instanceof SQLIntegrityConstraintViolationException sql) {
            return Result.error(409, friendlyDbMessage(sql.getMessage()));
        }
        return Result.error(409, "数据完整性冲突，请检查关联数据（如先删除引用它的记录）");
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("未处理异常", e);
        return Result.error(500, "系统开小差了（404 Not Found 小组正在修复），请稍后重试");
    }

    /** 提取 MySQL 触发器 SIGNAL 的中文提示 */
    private String friendlyDbMessage(String raw) {
        if (raw == null) {
            return "数据库约束冲突";
        }
        int idx = raw.indexOf('：');
        if (idx >= 0 && raw.contains("45000") || raw.length() < 120) {
            String candidate = idx >= 0 ? raw.substring(idx + 1) : raw;
            if (candidate.length() < 120) {
                return candidate.trim();
            }
        }
        return "数据库约束冲突，请检查输入数据";
    }
}
