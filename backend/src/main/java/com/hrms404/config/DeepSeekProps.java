package com.hrms404.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek V4 Flash API 配置（application.yml 的 deepseek.*）
 * API Key 通过环境变量 DEEPSEEK_API_KEY 注入，避免写死在代码/配置文件中
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProps {

    /** 接口 Base URL，如 https://api.deepseek.com */
    private String baseUrl = "https://api.deepseek.com";

    /** 模型名：deepseek-v4-flash */
    private String model = "deepseek-v4-flash";

    /** Bearer Token */
    private String apiKey = "";

    /** 单次请求读超时（秒） */
    private int readTimeoutSeconds = 30;
}
