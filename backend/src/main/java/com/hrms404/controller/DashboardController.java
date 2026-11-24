package com.hrms404.controller;

import com.hrms404.common.Result;
import com.hrms404.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计接口：按角色返回个性化卡片
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public Result<List<Map<String, Object>>> cards() {
        return Result.success(dashboardService.cards());
    }
}
