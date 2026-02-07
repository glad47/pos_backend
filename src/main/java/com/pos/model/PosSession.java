package com.pos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pos_sessions", indexes = {
    @Index(name = "idx_cashier", columnList = "cashier_name"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_opened_at", columnList = "opened_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PosSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_number", nullable = false)
    private Integer sessionNumber = 1;

    @Column(name = "cashier_name", nullable = false)
    private String cashierName;

    @Column(name = "opening_cash", nullable = false, precision = 10, scale = 2)
    private BigDecimal openingCash = BigDecimal.ZERO;

    @Column(name = "closing_cash", precision = 10, scale = 2)
    private BigDecimal closingCash;

    @Column(name = "total_sales", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "opened_at", updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public enum SessionStatus {
        OPEN, CLOSED
    }

    public boolean isActive() {
        return this.status == SessionStatus.OPEN && this.closedAt == null;
    }

}
