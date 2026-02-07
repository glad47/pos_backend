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
    @Index(name = "idx_loyalty_dates", columnList = "active, start_date, end_date"),
    @Index(name = "idx_odoo_program_id", columnList = "odoo_program_id"),
    @Index(name = "idx_odoo_rule_id", columnList = "odoo_rule_id")
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
     * 0 = DISCOUNT (fixed amount or percentage discount when eligible products meet min_qty)
     * 1 = BUY_X_GET_Y (buy trigger products, get reward products free)
     */
    @Column(nullable = false)
    private Integer type = 0;

    /**
     * Comma-separated list of product barcodes that trigger/participate in this loyalty.
     * For CSV programs: the eligible products. Customer buys min_qty of ANY combo from this list.
     */
    @Column(name = "trigger_product_ids", length = 5000)
    private String triggerProductIds;

    /**
     * Comma-separated list of product barcodes eligible for the reward.
     * For CSV programs: same as trigger_product_ids.
     * For BUY_X_GET_Y: different products that become free.
     */
    @Column(name = "reward_product_ids", length = 5000)
    private String rewardProductIds;

    /**
     * Minimum quantity of eligible products needed to activate.
     * E.g., "Buy 5 for 5.95" => min_quantity = 5.
     */
    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity = 1;

    @Column(name = "max_quantity", nullable = false)
    private Integer maxQuantity = 1;

    /**
     * Number of reward items given free (for BUY_X_GET_Y) per activation.
     */
    @Column(name = "reward_quantity", nullable = false)
    private Integer rewardQuantity = 1;

    /**
     * Discount percentage (for percentage-based discounts).
     */
    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    /**
     * Fixed total discount amount for the full set (min_qty items).
     * discount_amount = (totalPrice * min_qty) - afterDiscount
     */
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    /**
     * The total price the customer pays for the full set (min_qty items).
     * E.g., "Buy 5 for 5.95" => afterDiscount = 5.95.
     */
    @Column(name = "after_discount", precision = 10, scale = 2)
    private BigDecimal afterDiscount;

    /**
     * The unit price of the product(s) in the offer (before discount).
     */
    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "odoo_program_id")
    private Long odooProgramId;

    @Column(name = "odoo_rule_id")
    private Long odooRuleId;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

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

    @Transient
    public boolean isFixedDiscount() {
        return discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0;
    }
}
