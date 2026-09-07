package com.hrms404.common;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 通用 Excel 导出请求体：
 * columns 定义列（key 对应 rows 中的字段名，label 为表头中文名），rows 为数据行
 */
@Data
public class ExportRequest {

    @Data
    public static class Column {
        private String key;
        private String label;
    }

    private List<Column> columns;
    private List<Map<String, Object>> rows;

    /** 导出文件名（含 .xlsx 后缀），缺省用"导出数据.xlsx" */
    private String filename;
}
