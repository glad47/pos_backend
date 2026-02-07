package com.pos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_product_id", columnList = "product_id"),
    @Index(name = "idx_item_product_barcode", columnList = "product_barcode")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"order", "product"})
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @Column(name = "product_barcode", nullable = false, length = 50)
    private String productBarcode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    // Discount field - used by OrderService
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    // Total price field - used by OrderService
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "free_items", nullable = false)
    private Integer freeItems = 0;

    // Promotion name - used to track which promotion was applied
    @Column(name = "promotion_name", length = 255)
    private String promotionName;

    // Legacy field for compatibility
    @Column(name = "promotion_applied")
    private String promotionApplied;

    // Is this item a reward (free item from Buy X Get Y promotion)
    @Column(name = "is_reward", nullable = false)
    private boolean isReward = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Backward compatibility getters/setters
    public BigDecimal getDiscountAmount() {
        return discount;
    }

    public void setDiscountAmount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTotalAmount() {
        return totalPrice;
    }

    public void setTotalAmount(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
