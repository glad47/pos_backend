package com.pos.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLoginDTO {
    private String employeeId; // Barcode scanned
    private String pin; // Password entered
}
