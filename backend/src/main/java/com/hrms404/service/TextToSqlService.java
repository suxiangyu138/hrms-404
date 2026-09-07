package com.hrms404.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms404.common.BizException;
import com.hrms404.config.DeepSeekProps;
import com.hrms404.mapper.StatMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Text to SQL 服务：
 * 1. 读取 information_schema 拼装系统提示词（真实表结构，与数据库同步）
 * 2. 调用 DeepSeek V4 Flash API（OpenAI 兼容协议 POST /chat/completions）
 * 3. SQL 安全白名单校验（仅 SELECT、禁系统库/危险函数、强制 LIMIT、单语句）
 * 4. 执行并把列名映射为数据库字段中文注释返回
 */
@Slf4j
@Service
public class TextToSqlService {

    private static final List<String> FORBIDDEN_WORDS = List.of(
            "drop", "delete", "update", "insert", "alter", "truncate", "create",
            "replace", "grant", "revoke", "rename", "call", "prepare", "execute",
            "load_file", "into outfile", "into dumpfile", "sleep(", "benchmark("
    );
    private static final int MAX_ROWS = 100;

    private final DeepSeekProps props;
    private final StatMapper statMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TextToSqlService(DeepSeekProps props, StatMapper statMapper, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.props = props;
        this.statMapper = statMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** 自然语言 → 查询结果（data 含 sql/columns/rows/count） */
    public Map<String, Object> ask(String question) {
        if (question == null || question.isBlank()) {
            throw BizException.badRequest("请输入你的查询问题");
        }
        if (question.length() > 200) {
            throw BizException.badRequest("问题过长（≤200字），请精简后重试");
        }
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw BizException.badRequest("未配置 DEEPSEEK_API_KEY 环境变量：AI 服务暂不可用。"
                    + "请在 platform.deepseek.com 申请 Key 后以环境变量方式启动应用");
        }

        // 1. 真实表结构 → 系统提示词
        List<Map<String, Object>> meta = statMapper.tableColumnMeta();
        String schemaText = buildSchemaText(meta);
        String sql = chatForSql(schemaText, question.trim());

        // 2. 安全校验 + 执行 + 结果组装（与 runUserSql 共用同一通道）
        return runUserSql(sql);
    }

    /**
     * 执行 SQL（AI 生成或用户在页面上手动修正后重跑共用此通道）：
     * 任何语句都必须先通过安全白名单校验，不允许绕过
     */
    public Map<String, Object> runUserSql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw BizException.badRequest("SQL 不能为空");
        }
        List<Map<String, Object>> meta = statMapper.tableColumnMeta();
        String safeSql = validateAndForceLimit(sql);
        log.info("Text to SQL 执行（已通过安全校验）：{}", safeSql);

        long start = System.currentTimeMillis();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(safeSql);
        if (rows.size() > MAX_ROWS) {
            rows = new ArrayList<>(rows.subList(0, MAX_ROWS));
        }
        long cost = System.currentTimeMillis() - start;

        // 列名 → 中文注释
        Map<String, String> commentMap = buildCommentMap(meta);
        List<Map<String, Object>> columns = new ArrayList<>();
        if (!rows.isEmpty()) {
            for (String key : rows.get(0).keySet()) {
                Map<String, Object> col = new LinkedHashMap<>();
                col.put("key", key);
                col.put("label", commentMap.getOrDefault(key.toLowerCase(), key));
                columns.add(col);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sql", safeSql);
        data.put("columns", columns);
        data.put("rows", rows);
        data.put("count", rows.size());
        data.put("costMs", cost);
        return data;
    }

    /** 调用 DeepSeek Chat Completions，返回模型给出的 SQL（可能不合法，之后统一校验） */
    private String chatForSql(String schemaText, String question) {
        try {
            Map<String, Object> sys = Map.of(
                    "role", "system",
                    "content", "你是精通 MySQL 的数据库工程师，服务于人事管理系统（HRMS-404）。\n"
                            + "以下是与数据库完全同步的真实表结构（列名后为中文注释）：\n" + schemaText
                            + "\n约束：只回答一条可直接执行的 SQL 查询语句，不要输出任何解释、前后缀、Markdown 代码块标记。"
                            + "只能 SELECT，禁止任何写操作语句；不要访问 information_schema/mysql 等系统库；"
                            + "查全部数据时必须使用 LIMIT 防止超大数据量。");
            Map<String, Object> user = Map.of("role", "user", "content", question);
            Map<String, Object> body = Map.of(
                    "model", props.getModel(),
                    "messages", List.of(sys, user),
                    "temperature", 0,
                    "max_tokens", 4000,
                    "stream", false,
                    // 关键：强制关闭思考模式。V4 Flash 未显式指定时可能返回 reasoning_content，
                    // 复杂问题下推理过程会耗尽 max_tokens 导致 content 为空（"AI 未返回有效 SQL"）。
                    "thinking_mode", "non-thinking"
            );

            // DeepSeek 偶发返回空 content（同为 non-thinking 也有概率出现），最多尝试 3 次
            String sql = null;
            for (int attempt = 1; attempt <= 3 && sql == null; attempt++) {
                if (attempt > 1) {
                    log.warn("DeepSeek 返回空内容，自动重试第 {} 次", attempt);
                    try {
                        Thread.sleep(800L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                }
                sql = callApi(body);
            }
            if (sql == null) {
                throw BizException.badRequest("AI 未返回有效 SQL，请换个说法重试");
            }
            return sql;
        } catch (BizException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            log.warn("DeepSeek 调用超时", e);
            throw BizException.badRequest("AI 服务响应超时（" + props.getReadTimeoutSeconds() + "s），请稍后重试");
        } catch (InterruptedException e) {
            // 只有真正被中断时才恢复中断标记，避免污染 Tomcat 复用线程
            Thread.currentThread().interrupt();
            log.warn("DeepSeek 调用被中断", e);
            throw BizException.badRequest("AI 服务调用被中断，请重试");
        } catch (java.io.IOException e) {
            log.warn("DeepSeek 调用网络失败", e);
            throw BizException.badRequest("AI 服务网络异常，请检查网络后重试");
        } catch (Exception e) {
            log.error("DeepSeek 调用异常", e);
            throw BizException.badRequest("AI 服务解析异常：" + e.getMessage());
        }
    }

    /** 单次调用 /chat/completions：返回清理后的 SQL 文本；content 为空返回 null；HTTP 错误抛业务异常 */
    private String callApi(Map<String, Object> body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.getBaseUrl() + "/chat/completions"))
                .timeout(Duration.ofSeconds(props.getReadTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + props.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            String msg = extractApiError(resp.statusCode(), resp.body());
            throw BizException.badRequest(msg);
        }
        JsonNode root = objectMapper.readTree(resp.body());
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        String text = content.isMissingNode() ? "" : content.asText();
        if (text.isBlank()) {
            // 只记录结束原因便于排查，不落原始响应/问题内容
            JsonNode finish = root.path("choices").path(0).path("finish_reason");
            log.warn("DeepSeek 返回空 content（finish_reason={}）", finish.asText("unknown"));
            return null;
        }
        return stripCodeFence(text);
    }

    /** SQL 安全校验：仅 SELECT/WITH 开头、禁危险关键词、单条语句、强制 LIMIT */
    private String validateAndForceLimit(String raw) {
        String sql = raw == null ? "" : raw.trim().replaceAll(";\\s*$", "");
        String upper = sql.toUpperCase();
        if (!upper.startsWith("SELECT") && !upper.startsWith("WITH")) {
            throw BizException.badRequest("AI 未生成合法查询：仅允许 SELECT 查询语句（已拦截）");
        }
        String lower = sql.toLowerCase();
        for (String word : FORBIDDEN_WORDS) {
            // 单词边界防误伤（如列名 xxx_update 不会被命中）
            if (lower.matches("(?s).*\\b" + java.util.regex.Pattern.quote(word) + "\\b.*")
                    || lower.contains(word)) {
                throw BizException.badRequest("AI 生成的 SQL 含危险关键词【" + word + "】，已拦截");
            }
        }
        for (String sys : List.of("information_schema", "performance_schema", "mysql.", "sys.")) {
            if (lower.contains(sys)) {
                throw BizException.badRequest("AI 尝试访问系统库，已拦截");
            }
        }
        if (sql.indexOf(';') >= 0) {
            throw BizException.badRequest("仅允许单条 SQL 语句，已拦截多语句注入");
        }
        if (!upper.contains("LIMIT")) {
            sql = sql + "\nLIMIT " + MAX_ROWS;
        }
        return sql;
    }

    private String extractApiError(int status, String body) {
        String detail = "";
        try {
            detail = objectMapper.readTree(body).path("error").path("message").asText("");
        } catch (Exception ignored) {
        }
        String friendly = switch (status) {
            case 401 -> "API Key 无效，请检查 DEEPSEEK_API_KEY";
            case 402 -> "DeepSeek 账户余额不足，请充值后重试";
            case 429 -> "请求过于频繁，已被限流，请稍后重试";
            default -> "AI 服务返回错误（HTTP " + status + "）";
        };
        return friendly + (detail.isBlank() ? "" : "：" + detail);
    }

    private String stripCodeFence(String content) {
        String c = content.trim();
        if (c.startsWith("```")) {
            c = c.replaceAll("^```(sql|SQL)?\\s*", "").replaceAll("\\s*```$", "");
        }
        // 去掉可能的前缀说明（第一个 SELECT 之前的内容）
        int idx = c.toUpperCase().indexOf("SELECT");
        int idx2 = c.toUpperCase().indexOf("WITH");
        int start = Math.min(idx < 0 ? Integer.MAX_VALUE : idx, idx2 < 0 ? Integer.MAX_VALUE : idx2);
        if (start != Integer.MAX_VALUE) {
            c = c.substring(start);
        }
        return c.trim();
    }

    private String buildSchemaText(List<Map<String, Object>> meta) {
        StringBuilder sb = new StringBuilder();
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : meta) {
            grouped.computeIfAbsent(value(row, "TABLE_NAME"), k -> new ArrayList<>()).add(row);
        }
        for (Map.Entry<String, List<Map<String, Object>>> e : grouped.entrySet()) {
            sb.append("表 ").append(e.getKey()).append(" 的列：\n");
            for (Map<String, Object> col : e.getValue()) {
                sb.append("  - ").append(value(col, "COLUMN_NAME"))
                        .append(" 类型 ").append(value(col, "DATA_TYPE"))
                        .append(" 含义[").append(value(col, "COLUMN_COMMENT")).append(']')
                        .append('\n');
            }
        }
        return sb.toString();
    }

    private Map<String, String> buildCommentMap(List<Map<String, Object>> meta) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map<String, Object> col : meta) {
            String name = value(col, "COLUMN_NAME");
            String comment = value(col, "COLUMN_COMMENT");
            if (name != null && !name.isBlank()) {
                map.put(name.toLowerCase(), comment == null || comment.isBlank() ? name : comment);
            }
        }
        return map;
    }

    /** 大小写不敏感地从 information_schema 结果取列值 */
    private String value(Map<String, Object> row, String key) {
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key) && e.getValue() != null) {
                return String.valueOf(e.getValue());
            }
        }
        return "";
    }
}
