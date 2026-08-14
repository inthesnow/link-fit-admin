package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.CrmSale;
import com.linkfit.admin.mapper.CrmSalesMapper;
import com.linkfit.admin.security.CrmUserDetails;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/crm-sales")
public class CrmSalesApiController {

    private static final Logger log = LoggerFactory.getLogger(CrmSalesApiController.class);

    private final CrmSalesMapper mapper;

    public CrmSalesApiController(CrmSalesMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String salesType,
            @RequestParam(defaultValue = "") String startDate,
            @RequestParam(defaultValue = "") String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmSales] GET /api/crm-sales - salesType={}, startDate={}", salesType, startDate);
        Long gymId = gymId(principal);
        int offset = page * size;
        List<CrmSale> items = mapper.findAll(gymId, salesType, startDate, endDate, offset, size);
        long total = mapper.count(gymId, salesType, startDate, endDate);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", total);
        return ApiResponse.ok(data);
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(
            @RequestParam(required = false) String monthYear,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmSales] GET /api/crm-sales/summary - monthYear={}", monthYear);
        Long gymId = gymId(principal);
        String month = (monthYear != null && !monthYear.isEmpty())
                ? monthYear
                : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<Map<String, Object>> breakdown = mapper.monthlySummary(gymId, month);
        BigDecimal target = mapper.findTarget(gymId, month);

        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : breakdown) {
            Object t = row.get("total");
            if (t instanceof BigDecimal bd) total = total.add(bd);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monthYear", month);
        data.put("total", total);
        data.put("target", target != null ? target : BigDecimal.ZERO);
        data.put("achievement", target != null && target.compareTo(BigDecimal.ZERO) > 0
                ? total.multiply(new BigDecimal("100")).divide(target, 1, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        data.put("breakdown", breakdown);
        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody CrmSale sale,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmSales] POST /api/crm-sales");
        sale.setId(UUID.randomUUID().toString());
        sale.setGymId(gymId(principal));
        if (sale.getSaleDate() == null || sale.getSaleDate().isEmpty()) {
            sale.setSaleDate(LocalDate.now().toString());
        }
        mapper.insert(sale);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmSales] DELETE /api/crm-sales/{id} - id={}", id);
        if (mapper.delete(id, gymId(principal)) == 0) {
            return ApiResponse.error("매출 내역을 찾을 수 없습니다.");
        }
        return ApiResponse.ok();
    }

    @GetMapping("/target")
    public ApiResponse<Map<String, Object>> getTarget(
            @RequestParam(required = false) String monthYear,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmSales] GET /api/crm-sales/target - monthYear={}", monthYear);
        Long gymId = gymId(principal);
        String month = (monthYear != null && !monthYear.isEmpty())
                ? monthYear
                : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        BigDecimal target = mapper.findTarget(gymId, month);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monthYear", month);
        data.put("target", target != null ? target : BigDecimal.ZERO);
        return ApiResponse.ok(data);
    }

    @PutMapping("/target")
    public ApiResponse<Void> setTarget(@RequestBody Map<String, Object> body,
                                        @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmSales] PUT /api/crm-sales/target");
        Long gymId = gymId(principal);
        String month = (String) body.get("monthYear");
        BigDecimal target = new BigDecimal(body.get("target").toString());
        mapper.upsertTarget(gymId, month, target);
        return ApiResponse.ok();
    }

    @GetMapping("/export")
    public void export(
            @RequestParam(defaultValue = "") String salesType,
            @RequestParam(defaultValue = "") String startDate,
            @RequestParam(defaultValue = "") String endDate,
            @RequestParam(required = false) String monthYear,
            @AuthenticationPrincipal CrmUserDetails principal,
            HttpServletResponse response) throws IOException {
        log.info("[CrmSales] GET /api/crm-sales/export - salesType={}, startDate={}", salesType, startDate);
        Long gymId = gymId(principal);
        if (monthYear != null && !monthYear.isEmpty()) {
            startDate = monthYear + "-01";
            LocalDate ld = LocalDate.parse(startDate);
            endDate = ld.withDayOfMonth(ld.lengthOfMonth()).toString();
        }
        List<CrmSale> items = mapper.findAll(gymId, salesType, startDate, endDate, 0, 100_000);

        String filename = "crm-sales-" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("매출 내역");
            String[] headers = { "날짜", "회원명", "매출유형", "등록유형", "트레이너", "금액", "메모" };
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) hRow.createCell(i).setCellValue(headers[i]);
            int rowIdx = 1;
            for (CrmSale s : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(s.getSaleDate()     != null ? s.getSaleDate()     : "");
                row.createCell(1).setCellValue(s.getMemberName()   != null ? s.getMemberName()   : "");
                row.createCell(2).setCellValue(s.getSalesType()    != null ? s.getSalesType()    : "");
                row.createCell(3).setCellValue(s.getRegType()      != null ? s.getRegType()      : "");
                row.createCell(4).setCellValue(s.getTrainerName()  != null ? s.getTrainerName()  : "");
                row.createCell(5).setCellValue(s.getAmount()       != null ? s.getAmount().doubleValue() : 0);
                row.createCell(6).setCellValue(s.getNote()         != null ? s.getNote()         : "");
            }
            wb.write(response.getOutputStream());
        }
    }

    private Long gymId(CrmUserDetails principal) {
        return principal != null ? principal.getGymId() : 1L;
    }
}
