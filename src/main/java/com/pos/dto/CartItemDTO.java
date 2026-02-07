package com.pos.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private String barcode;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal discount;
    private String promotionName;
    private Boolean isReward;
}