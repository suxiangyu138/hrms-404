package com.hrms404.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek API 配置（application.yml 的 deepseek.*）
 * 这里的默认值只是兜底，实际取值以 application.yml 为准（该文件支持环境变量覆盖）：
 * DEEPSEEK_API_KEY / DEEPSEEK_MODEL / DEEPSEEK_BASE_URL
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProps {

    /** 接口 Base URL，如 https://api.deepseek.com */
    private String baseUrl = "https://api.deepseek.com";

    /** 模型名：deepseek-flash（旧名 deepseek-v4-flash 已下线，勿再使用） */
    private String model = "deepseek-flash";

    /** Bearer Token */
    private String apiKey = "";

    /** 单次请求读超时（秒） */
    private int readTimeoutSeconds = 30;
}
