package com.pos.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderDTO {
    private Long sessionId;
    private List<CartItemDTO> items;
    private String paymentMethod;
    private String cashierName;
}
