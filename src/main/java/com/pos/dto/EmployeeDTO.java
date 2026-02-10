package com.pos.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private Long id;
    private String employeeId;
    private String badgeId;
    private String name;
    private String pin;
    private Boolean saleUser;
    private Boolean returnUser;
    private Boolean managerUser;
    private Boolean active;
}
