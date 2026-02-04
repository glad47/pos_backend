package com.pos.service;

import com.pos.model.Loyalty;
import com.pos.model.Product;
import com.pos.repository.LoyaltyRepository;
import com.pos.repository.ProductRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
public class ExcelImportService {

    private final ProductRepository productRepository;
    private final LoyaltyRepository loyaltyRepository;

    public ExcelImportService(ProductRepository productRepository, LoyaltyRepository loyaltyRepository) {
        this.productRepository = productRepository;
        this.loyaltyRepository = loyaltyRepository;
    }

    /**
     * Import products from Excel file
     * Expected columns: Barcode, Name, Description, Price, Stock, Category, TaxRate
     */
    public List<Product> importProducts(MultipartFile file) throws Exception {
        List<Product> products = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            if (rows.hasNext()) rows.next(); // Skip header row
            
            while (rows.hasNext()) {
                Row row = rows.next();
                String barcode = getCellString(row.getCell(0));
                if (barcode == null || barcode.isEmpty()) continue;

                // Find existing or create new product
                Product product = productRepository.findByBarcode(barcode).orElse(new Product());
                product.setBarcode(barcode);
                product.setName(getCellString(row.getCell(1)));
                product.setDescription(getCellString(row.getCell(2)));
                product.setPrice(getCellBigDecimal(row.getCell(3)));
                
                Integer stock = getCellInt(row.getCell(4));
                product.setStock(stock != null ? stock : 0);
                
                product.setCategory(getCellString(row.getCell(5)));
                
                BigDecimal taxRate = getCellBigDecimal(row.getCell(6));
                product.setTaxRate(taxRate != null ? taxRate : BigDecimal.ZERO);
                
                product.setActive(true);
                
                products.add(productRepository.save(product));
            }
        }
        return products;
    }

    /**
     * Import loyalty programs from Excel file
     * Expected columns: Name, Type, BuyQty, FreeQty, DiscountPercent, ProductBarcode, Category, StartDate, EndDate
     */
    public List<Loyalty> importLoyaltyPrograms(MultipartFile file) throws Exception {
        List<Loyalty> loyalties = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            if (rows.hasNext()) rows.next(); // Skip header row
            
            while (rows.hasNext()) {
                Row row = rows.next();
                String name = getCellString(row.getCell(0));
                if (name == null || name.isEmpty()) continue;

                Loyalty loyalty = new Loyalty();
                loyalty.setName(name);
                
                // Parse type (BOGO, DISCOUNT, POINTS)
                String typeStr = getCellString(row.getCell(1));
                if (typeStr != null) {
                    try {
                        loyalty.setType(Loyalty.LoyaltyType.valueOf(typeStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        loyalty.setType(Loyalty.LoyaltyType.BOGO);
                    }
                } else {
                    loyalty.setType(Loyalty.LoyaltyType.BOGO);
                }
                
                Integer buyQty = getCellInt(row.getCell(2));
                loyalty.setBuyQuantity(buyQty != null ? buyQty : 1);
                
                Integer freeQty = getCellInt(row.getCell(3));
                loyalty.setFreeQuantity(freeQty != null ? freeQty : 1);
                
                BigDecimal discountPercent = getCellBigDecimal(row.getCell(4));
                loyalty.setDiscountPercent(discountPercent != null ? discountPercent : BigDecimal.ZERO);
                
                loyalty.setProductBarcode(getCellString(row.getCell(5)));
                loyalty.setCategory(getCellString(row.getCell(6)));
                
                // Parse dates
                LocalDateTime startDate = getCellDateTime(row.getCell(7));
                loyalty.setStartDate(startDate != null ? startDate : LocalDateTime.now());
                
                LocalDateTime endDate = getCellDateTime(row.getCell(8));
                loyalty.setEndDate(endDate != null ? endDate : LocalDateTime.now().plusYears(1));
                
                loyalty.setActive(true);
                
                loyalties.add(loyaltyRepository.save(loyalty));
            }
        }
        return loyalties;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private BigDecimal getCellBigDecimal(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;
        switch (cell.getCellType()) {
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING:
                try {
                    return new BigDecimal(cell.getStringCellValue().trim());
                } catch (Exception e) {
                    return BigDecimal.ZERO;
                }
            default:
                return BigDecimal.ZERO;
        }
    }

    private Integer getCellInt(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (Exception e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private LocalDateTime getCellDateTime(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue();
                }
                return null;
            case STRING:
                try {
                    String dateStr = cell.getStringCellValue().trim();
                    // Try common formats
                    DateTimeFormatter[] formatters = {
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    };
                    for (DateTimeFormatter formatter : formatters) {
                        try {
                            return LocalDateTime.parse(dateStr, formatter);
                        } catch (Exception ignored) {}
                    }
                    // Try date only
                    try {
                        return LocalDateTime.parse(dateStr + " 00:00:00", 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    } catch (Exception ignored) {}
                } catch (Exception e) {
                    return null;
                }
                return null;
            default:
                return null;
        }
    }
}
