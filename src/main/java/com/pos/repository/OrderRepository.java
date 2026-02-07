package com.pos.repository;

import com.pos.model.Order;
import com.pos.model.Order.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // Find orders by session ID
    List<Order> findBySessionId(Long sessionId);
    
    List<Order> findBySessionIdOrderByCreatedAtDesc(Long sessionId);
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    // Search by customer phone
    List<Order> findByCustomerPhoneContainingOrderByCreatedAtDesc(String phone);
    
    // Search by customer name
    List<Order> findByCustomerNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);
    
    // Search by customer VAT
    List<Order> findByCustomerVatContainingOrderByCreatedAtDesc(String vat);
    
    // Search by total amount range
    @Query("SELECT o FROM Order o WHERE o.totalAmount BETWEEN :minAmount AND :maxAmount ORDER BY o.createdAt DESC")
    List<Order> findByTotalAmountBetween(@Param("minAmount") BigDecimal minAmount, 
                                         @Param("maxAmount") BigDecimal maxAmount);
    
    // Search by order type
    List<Order> findByOrderTypeOrderByCreatedAtDesc(OrderType orderType);
    
    // Complex search query
    @Query("SELECT o FROM Order o WHERE " +
           "(:orderNumber IS NULL OR o.orderNumber LIKE %:orderNumber%) AND " +
           "(:customerPhone IS NULL OR o.customerPhone LIKE %:customerPhone%) AND " +
           "(:customerName IS NULL OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :customerName, '%'))) AND " +
           "(:customerVat IS NULL OR o.customerVat LIKE %:customerVat%) AND " +
           "(:minAmount IS NULL OR o.totalAmount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR o.totalAmount <= :maxAmount) AND " +
           "(:orderType IS NULL OR o.orderType = :orderType) " +
           "ORDER BY o.createdAt DESC")
    List<Order> searchOrders(@Param("orderNumber") String orderNumber,
                            @Param("customerPhone") String customerPhone,
                            @Param("customerName") String customerName,
                            @Param("customerVat") String customerVat,
                            @Param("minAmount") BigDecimal minAmount,
                            @Param("maxAmount") BigDecimal maxAmount,
                            @Param("orderType") OrderType orderType);
    
    // Find orders not synced
    List<Order> findBySyncStatusFalseOrderByCreatedAtDesc();
}
