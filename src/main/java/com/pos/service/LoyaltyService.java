package com.pos.service;

import com.pos.model.Loyalty;
import com.pos.repository.LoyaltyRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

                // Column order: Name, Type(0 or 1), TriggerProductIds, RewardProductIds,
                //               MinQuantity, RewardQuantity, DiscountPercent, Active, StartDate, EndDate
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
