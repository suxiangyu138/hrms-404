package com.hrms404.controller;

import com.hrms404.common.Result;
import com.hrms404.service.SalaryService;
import com.hrms404.vo.SalaryInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 薪资接口：/api/salaries
 * - 按月份列表、调用存储过程批量生成、绩效/扣款手工调整、个人薪资查询
 */
@RestController
@RequestMapping("/api/salaries")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;

    /** 某月薪资列表（视图 v_salary_history） */
    @GetMapping
    public Result<List<SalaryInfoVO>> listByMonth(@RequestParam String month) {
        return Result.success(salaryService.listByMonth(month));
    }

    /** 历史月份下拉选项 */
    @GetMapping("/months")
    public Result<List<String>> months() {
        return Result.success(salaryService.monthOptions());
    }

    /** 调用 sp_generate_monthly_salary 批量生成当月薪资（幂等：已有记录的月份跳过） */
    @PostMapping("/generate")
    public Result<Long> generate(@RequestBody Map<String, String> body) {
        long count = salaryService.generate(body.get("month"));
        return Result.success(count == 0
                ? "当月薪资已全部生成过（幂等，未新增重复记录）"
                : "调用存储过程生成成功，本次新增 " + count + " 条薪资记录", count);
    }

    /** 绩效/扣款调整（实发工资按触发器同规则重算） */
    @PutMapping("/{id}")
    public Result<Void> adjust(@PathVariable Long id,
                               @RequestBody Map<String, BigDecimal> body) {
        salaryService.adjust(id, body.get("performance"), body.get("deduction"));
        return Result.success("薪资调整成功（实发已按 基本+绩效-扣款 重算）", null);
    }

    /** 当前登录员工本人的薪资历史（个人中心） */
    @GetMapping("/my")
    public Result<List<SalaryInfoVO>> mySalaries() {
        return Result.success(salaryService.mySalaries());
    }
}
