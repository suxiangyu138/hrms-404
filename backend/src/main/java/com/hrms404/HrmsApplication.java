package com.hrms404;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * HRMS-404 人事管理系统 —— 启动类
 * 404 Not Found 小组 · 人事管理系统数据库课程设计
 */
@SpringBootApplication
@MapperScan("com.hrms404.mapper")
public class HrmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrmsApplication.class, args);
        System.out.println("==========================================");
        System.out.println(" HRMS-404 已启动：http://localhost:8080/login");
        System.out.println(" 404 Not Found 小组 · 页面已找到，系统运行正常 :)");
        System.out.println("==========================================");
    }
}
