package com.pos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "loyalty_programs", indexes = {
    @Index(name = "idx_loyalty_type", columnList = "type"),
    @Index(name = "idx_loyalty_active", columnList = "active"),
    @Index(name = "idx_loyalty_dates", columnList = "active, start_date, end_date")
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

    /**
     * Loyalty type:
     * 0 = DISCOUNT (percentage discount on reward products when trigger products are in cart)
     * 1 = BUY_X_GET_Y (buy trigger products, get reward products free)
     */
    @Column(nullable = false)
    private Integer type = 0;

    /**
     * Comma-separated list of product barcodes that trigger this loyalty.
     * When any of these products are in the cart, the loyalty may be applied.
     */
    @Column(name = "trigger_product_ids", length = 1000)
    private String triggerProductIds;

    /**
     * Comma-separated list of product barcodes that are eligible for the reward.
     * For BUY_X_GET_Y: these products become free.
     * For DISCOUNT: these products get the discount applied.
     */
    @Column(name = "reward_product_ids", length = 1000)
    private String rewardProductIds;

    /**
     * Minimum quantity of trigger product needed to activate loyalty.
     * For BUY_X_GET_Y: buy this many trigger products to get reward.
     * For DISCOUNT: minimum quantity of trigger product in cart.
     */
    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity = 1;

    /**
     * Number of reward items given free (for BUY_X_GET_Y) per activation.
     */
    @Column(name = "reward_quantity", nullable = false)
    private Integer rewardQuantity = 1;

    /**
     * Discount percentage (for type=0 DISCOUNT).
     * e.g. 10.00 means 10% off the reward products.
     */
    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---- Helper methods (not persisted) ----

    @Transient
    public List<String> getTriggerBarcodes() {
        if (triggerProductIds == null || triggerProductIds.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(triggerProductIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Transient
    public List<String> getRewardBarcodes() {
        if (rewardProductIds == null || rewardProductIds.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(rewardProductIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Transient
    public boolean isDiscount() {
        return type != null && type == 0;
    }

    @Transient
    public boolean isBuyXGetY() {
        return type != null && type == 1;
    }
}
