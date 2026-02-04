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
@Table(name = "promotions", indexes = {
    @Index(name = "idx_discount_type", columnList = "discount_type"),
    @Index(name = "idx_promo_active_dates", columnList = "active, start_date, end_date"),
    @Index(name = "idx_promo_product_barcode", columnList = "product_barcode"),
    @Index(name = "idx_promo_category", columnList = "category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType = DiscountType.PERCENTAGE;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(name = "min_purchase", precision = 10, scale = 2)
    private BigDecimal minPurchase = BigDecimal.ZERO;

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

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

    public enum DiscountType {
        PERCENTAGE, FIXED_AMOUNT
    }
}
