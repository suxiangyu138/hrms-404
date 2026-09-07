package com.hrms404.controller;

import com.hrms404.common.Result;
import com.hrms404.entity.Position;
import com.hrms404.service.PositionService;
import com.hrms404.vo.PositionInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 职位 RESTful 接口：/api/positions（编制预算由触发器 trg_check_budget 校验）
 */
@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    /** 职位列表（含部门名、编制占用数） */
    @GetMapping
    public Result<List<PositionInfoVO>> list() {
        return Result.success(positionService.listAll());
    }

    @PostMapping
    public Result<Void> create(@RequestBody Position position) {
        positionService.create(position);
        return Result.success("新增职位成功", null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Position position) {
        positionService.update(id, position);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        positionService.delete(id);
        return Result.success("职位已删除", null);
    }
}
