package com.pos.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSearchDTO {
    private String orderNumber;
    private String customerPhone;
    private String customerName;
    private String customerVat;
    private Double totalAmountMin;
    private Double totalAmountMax;
    private String orderType; // SALE or RETURN
}
