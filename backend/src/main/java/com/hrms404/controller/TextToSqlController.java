package com.hrms404.controller;

import com.hrms404.common.BizException;
import com.hrms404.common.Roles;
import com.hrms404.common.Result;
import com.hrms404.service.TextToSqlService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Text to SQL 接口：自然语言查询 + 修正后 SQL 重跑
 * 仅 ADMIN / HR / MANAGER 角色可用；SQL 安全校验在服务层统一完成
 */
@RestController
@RequestMapping("/api/text2sql")
@RequiredArgsConstructor
public class TextToSqlController {

    private final TextToSqlService textToSqlService;

    /** 自然语言 → DeepSeek V4 Flash → 安全校验 → 执行 → 结果表格 */
    @PostMapping
    public Result<Map<String, Object>> ask(@RequestBody Map<String, String> body) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR, Roles.MANAGER));
        return Result.success(textToSqlService.ask(body.get("question")));
    }

    /** 执行页面上已展示的 SQL（用户可修正后重跑，同样走白名单校验） */
    @PostMapping("/run-sql")
    public Result<Map<String, Object>> runSql(@RequestBody Map<String, String> body) {
        Roles.require(Roles.of(Roles.ADMIN, Roles.HR, Roles.MANAGER));
        try {
            return Result.success(textToSqlService.runUserSql(body.get("sql")));
        } catch (BizException e) {
            return Result.error(400, "SQL 校验失败：" + e.getMessage());
        }
    }
}
