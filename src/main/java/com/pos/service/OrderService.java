package com.pos.service;

import com.pos.dto.CartItemDTO;
import com.pos.dto.CreateOrderDTO;
import com.pos.model.*;
import com.pos.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PosSessionRepository sessionRepository;

    @Autowired
    private LoyaltyRepository loyaltyRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Transactional
    public Order createOrder(CreateOrderDTO dto) {
        // Validate session
        PosSession session = sessionRepository.findById(dto.getSessionId())
            .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getStatus() == PosSession.SessionStatus.CLOSED) {
            throw new RuntimeException("Cannot create order - session is closed");
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setSession(session);
        order.setPaymentMethod(Order.PaymentMethod.valueOf(dto.getPaymentMethod()));
        order.setCashierName(dto.getCashierName());
        order.setStatus(Order.OrderStatus.COMPLETED);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<String> appliedPromotions = new ArrayList<>();

        for (CartItemDTO cartItem : dto.getItems()) {
            Product product = productRepository.findByBarcode(cartItem.getBarcode())
                .orElseThrow(() -> new RuntimeException("Product not found: " + cartItem.getBarcode()));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setProductBarcode(product.getBarcode());
            item.setProductName(product.getName());
            item.setQuantity(cartItem.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setTaxRate(product.getTaxRate() != null ? product.getTaxRate() : BigDecimal.ZERO);

            // Calculate base subtotal
            BigDecimal itemSubtotal = product.getPrice().multiply(new BigDecimal(cartItem.getQuantity()));
            item.setSubtotal(itemSubtotal);

            // Apply BOGO loyalty
            int freeItems = applyBogo(product, cartItem.getQuantity(), appliedPromotions);
            item.setFreeItems(freeItems);

            // Calculate discount from BOGO
            BigDecimal bogoDiscount = product.getPrice().multiply(new BigDecimal(freeItems));
            
            // Apply percentage/fixed promotions
            BigDecimal promoDiscount = applyPromotions(product, itemSubtotal, appliedPromotions);
            
            BigDecimal itemDiscount = bogoDiscount.add(promoDiscount);
            item.setDiscountAmount(itemDiscount);

            // Calculate tax on discounted amount
            BigDecimal taxableAmount = itemSubtotal.subtract(itemDiscount);
            BigDecimal itemTax = taxableAmount.multiply(item.getTaxRate()).setScale(2, RoundingMode.HALF_UP);
            item.setTaxAmount(itemTax);

            // Calculate item total
            BigDecimal itemTotal = taxableAmount.add(itemTax);
            item.setTotalAmount(itemTotal);

            if (!appliedPromotions.isEmpty()) {
                item.setPromotionApplied(String.join(", ", appliedPromotions));
                appliedPromotions.clear(); // Clear for next item
            }

            orderItems.add(item);

            subtotal = subtotal.add(itemSubtotal);
            totalTax = totalTax.add(itemTax);
            totalDiscount = totalDiscount.add(itemDiscount);

            // NOTE: Not tracking inventory as per requirement
        }

        order.setItems(orderItems);
        order.setSubtotal(subtotal);
        order.setTaxAmount(totalTax);
        order.setDiscountAmount(totalDiscount);
        order.setTotalAmount(subtotal.subtract(totalDiscount).add(totalTax));

        // Update session totals
        session.setTotalSales(session.getTotalSales().add(order.getTotalAmount()));
        session.setTransactionCount(session.getTransactionCount() + 1);
        sessionRepository.save(session);

        return orderRepository.save(order);
    }

    private int applyBogo(Product product, int quantity, List<String> appliedPromotions) {
        List<Loyalty> loyalties = loyaltyRepository.findApplicableLoyalties(
            product.getBarcode(), 
            product.getCategory(), 
            LocalDateTime.now()
        );

        int totalFreeItems = 0;
        for (Loyalty loyalty : loyalties) {
            if (loyalty.getType() == Loyalty.LoyaltyType.BOGO && 
                loyalty.getBuyQuantity() != null && loyalty.getFreeQuantity() != null) {
                int sets = quantity / loyalty.getBuyQuantity();
                int freeItems = sets * loyalty.getFreeQuantity();
                totalFreeItems += freeItems;
                if (freeItems > 0) {
                    appliedPromotions.add(loyalty.getName() + " (+" + freeItems + " free)");
                }
            }
        }
        return totalFreeItems;
    }

    private BigDecimal applyPromotions(Product product, BigDecimal subtotal, List<String> appliedPromotions) {
        List<Promotion> promotions = promotionRepository.findApplicablePromotions(
            product.getBarcode(),
            product.getCategory(),
            LocalDateTime.now()
        );

        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (Promotion promotion : promotions) {
            BigDecimal discount = BigDecimal.ZERO;
            
            if (promotion.getDiscountType() == Promotion.DiscountType.PERCENTAGE && 
                promotion.getDiscountValue() != null) {
                discount = subtotal.multiply(promotion.getDiscountValue())
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                appliedPromotions.add(promotion.getName() + " (-" + promotion.getDiscountValue() + "%)");
            } else if (promotion.getDiscountType() == Promotion.DiscountType.FIXED_AMOUNT && 
                       promotion.getDiscountValue() != null) {
                discount = promotion.getDiscountValue().min(subtotal);
                appliedPromotions.add(promotion.getName() + " (-$" + discount + ")");
            }
            
            totalDiscount = totalDiscount.add(discount);
        }
        return totalDiscount;
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "ORD-" + timestamp + "-" + uuid;
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public List<Order> getOrdersBySession(Long sessionId) {
        return orderRepository.findBySessionId(sessionId);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
