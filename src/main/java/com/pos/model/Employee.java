package com.pos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "employees", indexes = {
    @Index(name = "idx_employee_id", columnList = "employee_id", unique = true),
    @Index(name = "idx_badge_id", columnList = "badge_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false, unique = true, length = 50)
    private String employeeId; // Used as barcode/username for login

    @Column(name = "badge_id", unique = true, length = 50)
    private String badgeId; // Original badge ID from Excel

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "pin", nullable = false, length = 10)
    private String pin; // PIN for authentication (stored as plain text for simplicity)

    @Column(name = "sale_user", nullable = false)
    private Boolean saleUser = false; // Can access POS sales

    @Column(name = "return_user", nullable = false)
    private Boolean returnUser = false; // Can access returns

    @Column(name = "manager_user", nullable = false)
    private Boolean managerUser = false; // Can manage users and close any session

    @Column(name = "active", nullable = false)
    private Boolean active = true; // Is employee active

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean canAccessSales() {
        return active && (saleUser || managerUser);
    }

    public boolean canAccessReturns() {
        return active && (returnUser || managerUser);
    }

    public boolean canManageUsers() {
        return active && managerUser;
    }

    public boolean canCloseSessions() {
        return active && managerUser;
    }

    public boolean isManager() {
        return active && managerUser;
    }
}
