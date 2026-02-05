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
import java.util.*;
import java.util.stream.Collectors;

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

        // Build cart map: barcode -> quantity
        Map<String, Integer> cartMap = new HashMap<>();
        Map<String, Product> productMap = new HashMap<>();
        for (CartItemDTO cartItem : dto.getItems()) {
            Product product = productRepository.findByBarcode(cartItem.getBarcode())
                .orElseThrow(() -> new RuntimeException("Product not found: " + cartItem.getBarcode()));
            cartMap.put(product.getBarcode(), cartItem.getQuantity());
            productMap.put(product.getBarcode(), product);
        }

        // Get active loyalties
        List<Loyalty> activeLoyalties = loyaltyRepository.findActiveLoyalties(LocalDateTime.now());

        // Calculate loyalty rewards applied to each reward barcode
        // Map: rewardBarcode -> list of applied loyalty descriptions and discount amounts
        Map<String, BigDecimal> rewardDiscounts = new HashMap<>();
        Map<String, Integer> rewardFreeItems = new HashMap<>();
        Map<String, List<String>> rewardPromoNames = new HashMap<>();

        for (Loyalty loyalty : activeLoyalties) {
            List<String> triggerBarcodes = loyalty.getTriggerBarcodes();
            List<String> rewardBarcodes = loyalty.getRewardBarcodes();

            if (triggerBarcodes.isEmpty() || rewardBarcodes.isEmpty()) continue;

            // Check if any trigger product is in the cart with sufficient quantity
            boolean triggered = false;
            int triggerSets = Integer.MAX_VALUE;
            for (String triggerBarcode : triggerBarcodes) {
                Integer qty = cartMap.get(triggerBarcode);
                if (qty != null && qty >= loyalty.getMinQuantity()) {
                    triggered = true;
                    int sets = qty / loyalty.getMinQuantity();
                    triggerSets = Math.min(triggerSets, sets);
                }
            }

            if (!triggered) continue;

            // Apply rewards to reward products that are in cart
            for (String rewardBarcode : rewardBarcodes) {
                if (!cartMap.containsKey(rewardBarcode)) continue;
                Product rewardProduct = productMap.get(rewardBarcode);
                if (rewardProduct == null) continue;

                if (loyalty.isBuyXGetY()) {
                    // BUY X GET Y: give free items
                    int freeQty = Math.min(triggerSets * loyalty.getRewardQuantity(), cartMap.get(rewardBarcode));
                    int existing = rewardFreeItems.getOrDefault(rewardBarcode, 0);
                    rewardFreeItems.put(rewardBarcode, existing + freeQty);
                    rewardPromoNames.computeIfAbsent(rewardBarcode, k -> new ArrayList<>())
                        .add(loyalty.getName() + " (+" + freeQty + " free)");
                } else if (loyalty.isDiscount()) {
                    // DISCOUNT: apply percentage discount
                    int rewardQty = Math.min(triggerSets * loyalty.getRewardQuantity(), cartMap.get(rewardBarcode));
                    BigDecimal discountableAmount = rewardProduct.getPrice().multiply(new BigDecimal(rewardQty));
                    BigDecimal discount = discountableAmount.multiply(loyalty.getDiscountPercent())
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                    BigDecimal existing = rewardDiscounts.getOrDefault(rewardBarcode, BigDecimal.ZERO);
                    rewardDiscounts.put(rewardBarcode, existing.add(discount));
                    rewardPromoNames.computeIfAbsent(rewardBarcode, k -> new ArrayList<>())
                        .add(loyalty.getName() + " (-" + loyalty.getDiscountPercent() + "%)");
                }
            }
        }

        // Build order items
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (CartItemDTO cartItem : dto.getItems()) {
            Product product = productMap.get(cartItem.getBarcode());
            
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setProductBarcode(product.getBarcode());
            item.setProductName(product.getName());
            item.setQuantity(cartItem.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setTaxRate(product.getTaxRate() != null ? product.getTaxRate() : BigDecimal.ZERO);

            BigDecimal itemSubtotal = product.getPrice().multiply(new BigDecimal(cartItem.getQuantity()));
            item.setSubtotal(itemSubtotal);

            // Apply loyalty rewards
            int freeItems = rewardFreeItems.getOrDefault(product.getBarcode(), 0);
            item.setFreeItems(freeItems);
            BigDecimal bogoDiscount = product.getPrice().multiply(new BigDecimal(freeItems));

            BigDecimal percentDiscount = rewardDiscounts.getOrDefault(product.getBarcode(), BigDecimal.ZERO);
            BigDecimal itemDiscount = bogoDiscount.add(percentDiscount);
            item.setDiscountAmount(itemDiscount);

            // Promotions applied label
            List<String> promoNames = rewardPromoNames.getOrDefault(product.getBarcode(), Collections.emptyList());
            if (!promoNames.isEmpty()) {
                item.setPromotionApplied(String.join(", ", promoNames));
            }

            BigDecimal taxableAmount = itemSubtotal.subtract(itemDiscount);
            if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) taxableAmount = BigDecimal.ZERO;
            BigDecimal itemTax = taxableAmount.multiply(item.getTaxRate()).setScale(2, RoundingMode.HALF_UP);
            item.setTaxAmount(itemTax);

            BigDecimal itemTotal = taxableAmount.add(itemTax);
            item.setTotalAmount(itemTotal);

            orderItems.add(item);

            subtotal = subtotal.add(itemSubtotal);
            totalTax = totalTax.add(itemTax);
            totalDiscount = totalDiscount.add(itemDiscount);
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
