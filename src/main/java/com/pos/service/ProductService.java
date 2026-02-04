package com.pos.service;

import com.pos.model.Product;
import com.pos.repository.ProductRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findByActiveTrue();
    }

    public Optional<Product> getProductByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode);
    }

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        productRepository.findById(id).ifPresent(product -> {
            product.setActive(false);
            productRepository.save(product);
        });
    }

    public List<Product> importFromExcel(MultipartFile file) throws Exception {
        List<Product> products = new ArrayList<>();
        
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            
            // Skip header row
            if (rows.hasNext()) rows.next();
            
            while (rows.hasNext()) {
                Row row = rows.next();
                Product product = new Product();
                
                // Column order: Barcode, Name, Description, Price, Stock, Category, TaxRate
                product.setBarcode(getCellStringValue(row.getCell(0)));
                product.setName(getCellStringValue(row.getCell(1)));
                product.setDescription(getCellStringValue(row.getCell(2)));
                product.setPrice(new BigDecimal(getCellNumericValue(row.getCell(3))));
                product.setStock((int) getCellNumericValue(row.getCell(4)));
                product.setCategory(getCellStringValue(row.getCell(5)));
                product.setTaxRate(new BigDecimal(getCellNumericValue(row.getCell(6))));
                product.setActive(true);
                
                // Update if exists, else create new
                Optional<Product> existing = productRepository.findByBarcode(product.getBarcode());
                if (existing.isPresent()) {
                    Product existingProduct = existing.get();
                    existingProduct.setName(product.getName());
                    existingProduct.setDescription(product.getDescription());
                    existingProduct.setPrice(product.getPrice());
                    existingProduct.setStock(product.getStock());
                    existingProduct.setCategory(product.getCategory());
                    existingProduct.setTaxRate(product.getTaxRate());
                    products.add(productRepository.save(existingProduct));
                } else {
                    products.add(productRepository.save(product));
                }
            }
        }
        
        return products;
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
