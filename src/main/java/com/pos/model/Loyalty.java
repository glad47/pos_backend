package com.pos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_programs", indexes = {
    @Index(name = "idx_type", columnList = "type"),
    @Index(name = "idx_active_dates", columnList = "active, start_date, end_date"),
    @Index(name = "idx_product_barcode", columnList = "product_barcode"),
    @Index(name = "idx_category", columnList = "category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Loyalty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoyaltyType type = LoyaltyType.BOGO;

    @Column(name = "buy_quantity", nullable = false)
    private Integer buyQuantity = 1;

    @Column(name = "free_quantity", nullable = false)
    private Integer freeQuantity = 1;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "product_barcode", length = 50)
    private String productBarcode;

    @Column(length = 100)
    private String category;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum LoyaltyType {
        BOGO, DISCOUNT, POINTS
    }
}
