package com.pos.service;

import com.pos.model.Loyalty;
import com.pos.repository.LoyaltyRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LoyaltyService {

    @Autowired
    private LoyaltyRepository loyaltyRepository;

    public List<Loyalty> getAllLoyalties() {
        return loyaltyRepository.findAll();
    }

    public List<Loyalty> getActiveLoyalties() {
        return loyaltyRepository.findActiveLoyalties(LocalDateTime.now());
    }

    public Loyalty saveLoyalty(Loyalty loyalty) {
        return loyaltyRepository.save(loyalty);
    }

    public void deleteLoyalty(Long id) {
        loyaltyRepository.findById(id).ifPresent(loyalty -> {
            loyalty.setActive(false);
            loyaltyRepository.save(loyalty);
        });
    }

    /**
     * Bulk upsert loyalty programs (used by sync service).
     * Matches by odoo_program_id if present, otherwise by name.
     */
    public List<Loyalty> bulkUpsert(List<Loyalty> loyalties) {
        List<Loyalty> results = new ArrayList<>();
        for (Loyalty incoming : loyalties) {
            Loyalty existing = null;

            // Try to find by odoo_program_id first
            if (incoming.getOdooProgramId() != null) {
                existing = loyaltyRepository.findByOdooProgramId(incoming.getOdooProgramId()).orElse(null);
            }

            if (existing != null) {
                // Update existing
                existing.setName(incoming.getName());
                existing.setType(incoming.getType());
                existing.setTriggerProductIds(incoming.getTriggerProductIds());
                existing.setRewardProductIds(incoming.getRewardProductIds());
                existing.setMinQuantity(incoming.getMinQuantity());
                existing.setMaxQuantity(incoming.getMaxQuantity());
                existing.setRewardQuantity(incoming.getRewardQuantity());
                existing.setDiscountPercent(incoming.getDiscountPercent());
                existing.setDiscountAmount(incoming.getDiscountAmount());
                existing.setAfterDiscount(incoming.getAfterDiscount());
                existing.setTotalPrice(incoming.getTotalPrice());
                existing.setActive(incoming.getActive());
                existing.setStartDate(incoming.getStartDate());
                existing.setEndDate(incoming.getEndDate());
                existing.setOdooRuleId(incoming.getOdooRuleId());
                existing.setLastSyncAt(LocalDateTime.now());
                results.add(loyaltyRepository.save(existing));
            } else {
                // Create new
                incoming.setLastSyncAt(LocalDateTime.now());
                results.add(loyaltyRepository.save(incoming));
            }
        }
        return results;
    }

    /**
     * Import loyalty programs from CSV file (the Odoo loyalty export format).
     * Groups rows by program_id; collects all eligible_product_barcode into comma-separated list.
     * 
     * CSV columns used:
     *  - program_id (group key)
     *  - program_name
     *  - loyalty_program_total_price (unit price before discount)
     *  - loyalty_program_after_discount (total price for the set)
     *  - loyalty_program_discount (total discount for the set)
     *  - loyalty_program_minimum_qty (how many items needed)
     *  - rule_active (True/False)
     *  - eligible_product_barcode (one per row, grouped by program_id)
     *  - rule_id
     */
    public List<Loyalty> importFromCsv(MultipartFile file) throws Exception {
        Map<String, CsvProgramGroup> groups = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new RuntimeException("Empty CSV file");
            
            // Remove BOM if present
            headerLine = headerLine.replace("\uFEFF", "").trim();
            // Remove surrounding quotes and carriage return
            headerLine = headerLine.replaceAll("\r", "");
            
            String[] headers = parseCsvLine(headerLine);
            Map<String, Integer> colIdx = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colIdx.put(headers[i].replace("\"", "").trim(), i);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("\r", "").trim();
                if (line.isEmpty()) continue;
                
                String[] cols = parseCsvLine(line);
                
                String programId = getCol(cols, colIdx, "program_id");
                String programName = getCol(cols, colIdx, "program_name");
                String totalPrice = getCol(cols, colIdx, "loyalty_program_total_price");
                String afterDiscount = getCol(cols, colIdx, "loyalty_program_after_discount");
                String discount = getCol(cols, colIdx, "loyalty_program_discount");
                String minQty = getCol(cols, colIdx, "loyalty_program_minimum_qty");
                String ruleActive = getCol(cols, colIdx, "rule_active");
                String eligibleBarcode = getCol(cols, colIdx, "eligible_product_barcode");
                String ruleId = getCol(cols, colIdx, "rule_id");

                if (programId.isEmpty() || eligibleBarcode.isEmpty()) continue;

                CsvProgramGroup group = groups.computeIfAbsent(programId, k -> {
                    CsvProgramGroup g = new CsvProgramGroup();
                    g.programId = programId;
                    g.programName = programName;
                    g.totalPrice = totalPrice;
                    g.afterDiscount = afterDiscount;
                    g.discount = discount;
                    g.minQty = minQty;
                    g.ruleActive = ruleActive;
                    g.ruleId = ruleId;
                    return g;
                });
                group.eligibleBarcodes.add(eligibleBarcode);
            }
        }

        // Convert groups to Loyalty entities
        List<Loyalty> result = new ArrayList<>();
        for (CsvProgramGroup group : groups.values()) {
            Loyalty loyalty = new Loyalty();
            loyalty.setName(group.programName);
            loyalty.setType(0); // DISCOUNT (fixed amount)

            // Deduplicate barcodes
            String barcodes = group.eligibleBarcodes.stream()
                    .distinct()
                    .collect(Collectors.joining(","));
            loyalty.setTriggerProductIds(barcodes);
            loyalty.setRewardProductIds(barcodes); // Same group

            int minQ = parseIntSafe(group.minQty, 1);
            loyalty.setMinQuantity(minQ);
            loyalty.setMaxQuantity(1);
            loyalty.setRewardQuantity(minQ);

            BigDecimal tp = parseBigDecimalSafe(group.totalPrice);
            BigDecimal ad = parseBigDecimalSafe(group.afterDiscount);
            BigDecimal disc = parseBigDecimalSafe(group.discount);

            loyalty.setTotalPrice(tp);
            loyalty.setAfterDiscount(ad);
            loyalty.setDiscountAmount(disc);

            // Also calculate percentage for display
            BigDecimal fullPrice = tp.multiply(BigDecimal.valueOf(minQ));
            if (fullPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pct = disc.divide(fullPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                loyalty.setDiscountPercent(pct);
            } else {
                loyalty.setDiscountPercent(BigDecimal.ZERO);
            }

            loyalty.setActive("True".equalsIgnoreCase(group.ruleActive) || "true".equalsIgnoreCase(group.ruleActive));
            loyalty.setStartDate(LocalDateTime.now().minusYears(1));
            loyalty.setEndDate(LocalDateTime.now().plusYears(2));
            loyalty.setOdooProgramId(parseLongSafe(group.programId));
            loyalty.setOdooRuleId(parseLongSafe(group.ruleId));

            // Upsert by odoo_program_id
            Loyalty existing = loyalty.getOdooProgramId() != null 
                ? loyaltyRepository.findByOdooProgramId(loyalty.getOdooProgramId()).orElse(null) 
                : null;
            
            if (existing != null) {
                existing.setName(loyalty.getName());
                existing.setType(loyalty.getType());
                existing.setTriggerProductIds(loyalty.getTriggerProductIds());
                existing.setRewardProductIds(loyalty.getRewardProductIds());
                existing.setMinQuantity(loyalty.getMinQuantity());
                existing.setMaxQuantity(loyalty.getMaxQuantity());
                existing.setRewardQuantity(loyalty.getRewardQuantity());
                existing.setDiscountPercent(loyalty.getDiscountPercent());
                existing.setDiscountAmount(loyalty.getDiscountAmount());
                existing.setAfterDiscount(loyalty.getAfterDiscount());
                existing.setTotalPrice(loyalty.getTotalPrice());
                existing.setActive(loyalty.getActive());
                existing.setStartDate(loyalty.getStartDate());
                existing.setEndDate(loyalty.getEndDate());
                existing.setOdooRuleId(loyalty.getOdooRuleId());
                existing.setLastSyncAt(LocalDateTime.now());
                result.add(loyaltyRepository.save(existing));
            } else {
                loyalty.setLastSyncAt(LocalDateTime.now());
                result.add(loyaltyRepository.save(loyalty));
            }
        }

        return result;
    }

    /**
     * Simple CSV line parser that handles quoted fields.
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        
        return fields.toArray(new String[0]);
    }

    private String getCol(String[] cols, Map<String, Integer> colIdx, String name) {
        Integer idx = colIdx.get(name);
        if (idx == null || idx >= cols.length) return "";
        String val = cols[idx].replace("\"", "").trim();
        if ("NULL".equalsIgnoreCase(val)) return "";
        return val;
    }

    private int parseIntSafe(String val, int defaultVal) {
        try {
            return (int) Double.parseDouble(val);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private Long parseLongSafe(String val) {
        try {
            return (long) Double.parseDouble(val);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimalSafe(String val) {
        try {
            return new BigDecimal(val).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // Inner class for CSV grouping
    private static class CsvProgramGroup {
        String programId;
        String programName;
        String totalPrice;
        String afterDiscount;
        String discount;
        String minQty;
        String ruleActive;
        String ruleId;
        List<String> eligibleBarcodes = new ArrayList<>();
    }

    // ---- Excel import (existing) ----

    public List<Loyalty> importFromExcel(MultipartFile file) throws Exception {
        List<Loyalty> loyalties = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row
            if (rows.hasNext()) rows.next();

            while (rows.hasNext()) {
                Row row = rows.next();
                Loyalty loyalty = new Loyalty();

                loyalty.setName(getCellStringValue(row.getCell(0)));

                int typeVal = (int) getCellNumericValue(row.getCell(1));
                loyalty.setType(typeVal == 1 ? 1 : 0);

                loyalty.setTriggerProductIds(getCellStringValue(row.getCell(2)));
                loyalty.setRewardProductIds(getCellStringValue(row.getCell(3)));
                loyalty.setMinQuantity(Math.max(1, (int) getCellNumericValue(row.getCell(4))));
                loyalty.setRewardQuantity(Math.max(1, (int) getCellNumericValue(row.getCell(5))));
                loyalty.setDiscountPercent(BigDecimal.valueOf(getCellNumericValue(row.getCell(6))));

                String activeStr = getCellStringValue(row.getCell(7));
                loyalty.setActive(activeStr.isEmpty() || "1".equals(activeStr) || "true".equalsIgnoreCase(activeStr));

                String startDate = getCellStringValue(row.getCell(8));
                if (!startDate.isEmpty()) {
                    loyalty.setStartDate(LocalDateTime.parse(startDate, formatter));
                } else {
                    loyalty.setStartDate(LocalDateTime.now());
                }

                String endDate = getCellStringValue(row.getCell(9));
                if (!endDate.isEmpty()) {
                    loyalty.setEndDate(LocalDateTime.parse(endDate, formatter));
                } else {
                    loyalty.setEndDate(LocalDateTime.now().plusYears(1));
                }

                loyalties.add(loyaltyRepository.save(loyalty));
            }
        }

        return loyalties;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return "";
        }
    }

    private double getCellNumericValue(Cell cell) {
        if (cell == null) return 0.0;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
}
