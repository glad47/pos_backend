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

    public List<Loyalty> getApplicableLoyalties(String barcode, String category) {
        return loyaltyRepository.findApplicableLoyalties(barcode, category, LocalDateTime.now());
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

                // Column order: Name, Type, BuyQty, FreeQty, DiscountPercent, ProductBarcode, Category, StartDate, EndDate
                loyalty.setName(getCellStringValue(row.getCell(0)));
                
                // Parse type enum
                String typeStr = getCellStringValue(row.getCell(1));
                try {
                    loyalty.setType(Loyalty.LoyaltyType.valueOf(typeStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    loyalty.setType(Loyalty.LoyaltyType.BOGO);
                }
                
                loyalty.setBuyQuantity((int) getCellNumericValue(row.getCell(2)));
                loyalty.setFreeQuantity((int) getCellNumericValue(row.getCell(3)));
                loyalty.setDiscountPercent(BigDecimal.valueOf(getCellNumericValue(row.getCell(4))));
                
                String barcode = getCellStringValue(row.getCell(5));
                loyalty.setProductBarcode(barcode.isEmpty() ? null : barcode);
                
                String category = getCellStringValue(row.getCell(6));
                loyalty.setCategory(category.isEmpty() ? null : category);
                
                String startDate = getCellStringValue(row.getCell(7));
                if (!startDate.isEmpty()) {
                    loyalty.setStartDate(LocalDateTime.parse(startDate, formatter));
                } else {
                    loyalty.setStartDate(LocalDateTime.now());
                }
                
                String endDate = getCellStringValue(row.getCell(8));
                if (!endDate.isEmpty()) {
                    loyalty.setEndDate(LocalDateTime.parse(endDate, formatter));
                } else {
                    loyalty.setEndDate(LocalDateTime.now().plusYears(1));
                }
                
                loyalty.setActive(true);
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
