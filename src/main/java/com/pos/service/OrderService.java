package com.pos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pos.dto.CreateOrderDTO;
import com.pos.dto.OrderSearchDTO;
import com.pos.model.*;
import com.pos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PosSessionRepository sessionRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order createOrder(Long sessionId, CreateOrderDTO dto) throws Exception {
        PosSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.isActive()) {
            throw new RuntimeException("Session is not active");
        }

        Order order = new Order();
        order.setSession(session);
        order.setCashierName(session.getCashierName());
        
        // Set customer information
        order.setCustomerName(dto.getCustomerName());
        order.setCustomerPhone(dto.getCustomerPhone());
        order.setCustomerVat(dto.getCustomerVat());
        
        // Set order type
        Order.OrderType orderType = dto.getOrderType() != null && dto.getOrderType().equals("RETURN") 
            ? Order.OrderType.RETURN : Order.OrderType.SALE;
        order.setOrderType(orderType);
        
        // For return orders
        if (orderType == Order.OrderType.RETURN) {
            order.setOriginalOrderNumber(dto.getOriginalOrderNumber());
            order.setReturnReason(dto.getReturnReason());
            order.setStatus(Order.OrderStatus.REFUNDED);
        }

        // Generate order number
        String orderNumber = generateOrderNumber(session);
        order.setOrderNumber(orderNumber);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;

        // Process items
        for (var item : dto.getItems()) {
            Product product = productRepository.findByBarcode(item.getBarcode())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getBarcode()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductBarcode(item.getBarcode()); // Use barcode from DTO (frontend)
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setTaxRate(product.getTaxRate()); // FIX: Add tax rate
            
            // For return orders, make quantities negative
            int actualQuantity = orderType == Order.OrderType.RETURN ? -Math.abs(item.getQuantity()) : item.getQuantity();
            orderItem.setQuantity(actualQuantity);

            BigDecimal lineSubtotal = product.getPrice().multiply(BigDecimal.valueOf(Math.abs(actualQuantity)));
            BigDecimal lineDiscount = item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO;
            BigDecimal lineTax = lineSubtotal.subtract(lineDiscount)
                    .multiply(product.getTaxRate())
                    .setScale(2, RoundingMode.HALF_UP);

            orderItem.setSubtotal(lineSubtotal); // FIX: Add subtotal
            orderItem.setDiscount(lineDiscount);
            orderItem.setTaxAmount(lineTax);
            orderItem.setTotalPrice(lineSubtotal.subtract(lineDiscount).add(lineTax));
            orderItem.setPromotionName(item.getPromotionName());
            orderItem.setIsReward(item.getIsReward() != null ? item.getIsReward() : false);

            order.getItems().add(orderItem);

            subtotal = subtotal.add(lineSubtotal);
            discountTotal = discountTotal.add(lineDiscount);
            taxTotal = taxTotal.add(lineTax);
        }

        // For return orders, negate totals
        if (orderType == Order.OrderType.RETURN) {
            subtotal = subtotal.negate();
            discountTotal = discountTotal.negate();
            taxTotal = taxTotal.negate();
        }

        order.setSubtotal(subtotal);
        order.setDiscountAmount(discountTotal);
        order.setTaxAmount(taxTotal);
        order.setTotalAmount(subtotal.subtract(discountTotal).add(taxTotal));

        try {
            Order.PaymentMethod paymentMethod = Order.PaymentMethod.valueOf(dto.getPaymentMethod().toUpperCase());
            order.setPaymentMethod(paymentMethod);
        } catch (IllegalArgumentException e) {
            order.setPaymentMethod(Order.PaymentMethod.CASH);
        }

        order.setNotes(dto.getNotes());
        
        // Generate and store JSON
        String orderJson = generateOrderJson(order, dto);
        order.setOrderJson(orderJson);
        
        // Initially not synced
        order.setSyncStatus(Boolean.FALSE);

        Order savedOrder = orderRepository.save(order);

        // Update session totals
        session.setTotalSales(session.getTotalSales().add(order.getTotalAmount()));
        sessionRepository.save(session);

        return savedOrder;
    }

    private String generateOrderJson(Order order, CreateOrderDTO dto) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        
        if (order.getOrderType() == Order.OrderType.SALE) {
            root.put("draft", false);
            ArrayNode ordersArray = objectMapper.createArrayNode();
            
            ObjectNode orderNode = objectMapper.createObjectNode();
            orderNode.put("id", order.getOrderNumber());
            
            ObjectNode dataNode = objectMapper.createObjectNode();
            dataNode.put("name", "Order " + order.getOrderNumber());
            dataNode.put("amount_paid", order.getTotalAmount().doubleValue());
            dataNode.put("amount_total", order.getTotalAmount().doubleValue());
            dataNode.put("amount_tax", order.getTaxAmount().doubleValue());
            dataNode.put("amount_return", 0);
            
            // Customer info
            ObjectNode customerNode = objectMapper.createObjectNode();
            customerNode.put("phone", order.getCustomerPhone() != null ? order.getCustomerPhone() : "");
            customerNode.put("name", order.getCustomerName() != null ? order.getCustomerName() : "");
            if (order.getCustomerVat() != null && !order.getCustomerVat().isEmpty()) {
                customerNode.put("vat", order.getCustomerVat());
            }
            dataNode.set("customer", customerNode);
            
            // Order lines
            ArrayNode linesArray = objectMapper.createArrayNode();
            for (OrderItem item : order.getItems()) {
                ObjectNode lineNode = objectMapper.createObjectNode();
                lineNode.put("qty", item.getQuantity());
                
                // For reward items, show price as negative or 0 based on display preference
                if (item.getIsReward()) {
                    lineNode.put("price_unit", 0); // Show as 0 in invoice as per requirement
                } else {
                    lineNode.put("price_unit", item.getUnitPrice().doubleValue());
                }
                
                lineNode.put("product_id", item.getProduct().getId());
                lineNode.put("discount", item.getDiscount().doubleValue());
                
                if (item.getPromotionName() != null) {
                    lineNode.put("promotion", item.getPromotionName());
                }
                if (item.getIsReward()) {
                    lineNode.put("is_reward", true);
                }
                
                linesArray.add(lineNode);
            }
            dataNode.set("order_lines", linesArray);
            
            orderNode.set("data", dataNode);
            ordersArray.add(orderNode);
            root.set("orders", ordersArray);
            
        } else { // RETURN order
            ArrayNode returnsArray = objectMapper.createArrayNode();
            
            ObjectNode returnNode = objectMapper.createObjectNode();
            returnNode.put("sale_order_name", order.getOriginalOrderNumber());
            
            ArrayNode returnLinesArray = objectMapper.createArrayNode();
            for (OrderItem item : order.getItems()) {
                ObjectNode lineNode = objectMapper.createObjectNode();
                lineNode.put("qty", Math.abs(item.getQuantity())); // Always positive in return JSON
                lineNode.put("price_unit", item.getUnitPrice().doubleValue());
                lineNode.put("product_id", item.getProduct().getId());
                lineNode.put("discount", item.getDiscount().doubleValue());
                
                returnLinesArray.add(lineNode);
            }
            returnNode.set("return_lines", returnLinesArray);
            returnNode.put("reason", order.getReturnReason() != null ? order.getReturnReason() : "Customer return");
            
            returnsArray.add(returnNode);
            root.set("returns", returnsArray);
        }
        
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private String generateOrderNumber(PosSession session) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("ORD-%s-%s", session.getId(), timestamp);
    }

    public List<Order> getSessionOrders(Long sessionId) {
        return orderRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }

    public Optional<Order> getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Transactional
    public List<Order> searchOrders(OrderSearchDTO searchDTO) {
        BigDecimal minAmount = searchDTO.getTotalAmountMin() != null 
            ? BigDecimal.valueOf(searchDTO.getTotalAmountMin()) : null;
        BigDecimal maxAmount = searchDTO.getTotalAmountMax() != null 
            ? BigDecimal.valueOf(searchDTO.getTotalAmountMax()) : null;
        
        Order.OrderType orderType = null;
        if (searchDTO.getOrderType() != null) {
            try {
                orderType = Order.OrderType.valueOf(searchDTO.getOrderType().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid order type, will be treated as null
            }
        }
        
        return orderRepository.searchOrders(
            searchDTO.getOrderNumber(),
            searchDTO.getCustomerPhone(),
            searchDTO.getCustomerName(),
            searchDTO.getCustomerVat(),
            minAmount,
            maxAmount,
            orderType
        );
    }

    @Transactional
    public Order updateSyncStatus(Long orderId, Boolean synced) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setSyncStatus(synced);
        return orderRepository.save(order);
    }

    public List<Order> getUnsyncedOrders() {
        return orderRepository.findBySyncStatusFalseOrderByCreatedAtDesc();
    }
}
