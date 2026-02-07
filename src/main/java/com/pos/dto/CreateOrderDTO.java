package com.pos.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderDTO {
    private List<CartItemDTO> items;
    private String paymentMethod;
    private String notes;
    
    // Customer information
    private String customerName;
    private String customerPhone;
    private String customerVat;
    
    // Order type
    private String orderType; // "SALE" or "RETURN"
    
    // For return orders
    private String originalOrderNumber;
    private String returnReason;
}

