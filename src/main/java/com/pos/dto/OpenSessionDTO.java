package com.pos.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenSessionDTO {
    private String cashierName;
    private String employeeId;
    private BigDecimal openingCash;
}
