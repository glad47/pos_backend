package com.pos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_number", columnList = "order_number"),
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_created_at", columnList = "created_at"),
    @Index(name = "idx_payment_method", columnList = "payment_method"),
    @Index(name = "idx_customer_phone", columnList = "customer_phone"),
    @Index(name = "idx_customer_vat", columnList = "customer_vat"),
    @Index(name = "idx_order_type", columnList = "order_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"session", "items"})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private PosSession session;

    @Column(name = "session_id", insertable = false, updatable = false)
    private Long sessionId;

    @Column(name = "cashier_name", nullable = false)
    private String cashierName;

    // Customer Information
    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Column(name = "customer_vat", length = 50)
    private String customerVat;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"order"})
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.COMPLETED;

    // Order Type: SALE or RETURN
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType = OrderType.SALE;

    // Reference to original sale order for returns
    @Column(name = "original_order_number", length = 50)
    private String originalOrderNumber;

    @Column(name = "return_reason", length = 500)
    private String returnReason;

    // JSON storage for order data in specified format
    @Column(name = "order_json", columnDefinition = "TEXT")
    private String orderJson;

    // Sync status - tracks if order has been sent to backend
    @Column(name = "sync_status", nullable = false)
    private boolean syncStatus = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PaymentMethod {
        CASH, CARD, OTHER
    }

    public enum OrderStatus {
        PENDING, COMPLETED, CANCELLED, REFUNDED
    }

    public enum OrderType {
        SALE, RETURN
    }
}
