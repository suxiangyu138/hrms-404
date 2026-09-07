package com.hrms404.controller;

import com.hrms404.common.BizException;
import com.hrms404.common.ExportRequest;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 通用 Excel 导出接口：
 * 前端把已加载的数据（列定义 + 行数据）POST 过来，后端用 Apache POI 生成真正的 .xlsx 文件
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    @PostMapping("/xlsx")
    public ResponseEntity<byte[]> exportXlsx(@RequestBody ExportRequest request) throws IOException {
        if (request.getColumns() == null || request.getColumns().isEmpty()) {
            throw BizException.badRequest("导出列定义不能为空");
        }
        // 文件名防御：强制 .xlsx 后缀
        String rawName = request.getFilename() == null || request.getFilename().isBlank()
                ? "导出数据.xlsx" : request.getFilename().trim();
        if (!rawName.toLowerCase().endsWith(".xlsx")) {
            rawName += ".xlsx";
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("导出数据");

            // 表头样式：加粗 + 浅灰底
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < request.getColumns().size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(request.getColumns().get(i).getLabel());
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 16 * 256);
            }

            // 数据行：数字写数字单元格、字符串写文本单元格
            if (request.getRows() != null) {
                int rowIdx = 1;
                for (Map<String, Object> data : request.getRows()) {
                    Row row = sheet.createRow(rowIdx++);
                    for (int i = 0; i < request.getColumns().size(); i++) {
                        Object value = data.get(request.getColumns().get(i).getKey());
                        Cell cell = row.createCell(i);
                        if (value == null) {
                            cell.setBlank();
                        } else if (value instanceof Number number) {
                            cell.setCellValue(number.doubleValue());
                        } else if (value instanceof Boolean bool) {
                            cell.setCellValue(bool);
                        } else {
                            cell.setCellValue(String.valueOf(value));
                        }
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            // Content-Disposition 双写：ASCII 兜底文件名 + RFC5987 中文文件名，兼容所有浏览器
            String encoded = URLEncoder.encode(rawName, StandardCharsets.UTF_8).replace("+", "%20");
            String disposition = "attachment; filename=\"export.xlsx\"; filename*=UTF-8''" + encoded;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }
}
