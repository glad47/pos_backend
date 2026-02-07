package com.pos.controller;

import com.pos.dto.CreateOrderDTO;
import com.pos.dto.OrderSearchDTO;
import com.pos.model.Order;
import com.pos.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/session/{sessionId}")
    public ResponseEntity<?> createOrder(@PathVariable Long sessionId, @RequestBody CreateOrderDTO dto) {
        try {
            Order order = orderService.createOrder(sessionId, dto);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<Order>> getSessionOrders(@PathVariable Long sessionId) {
        return ResponseEntity.ok(orderService.getSessionOrders(sessionId));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        return orderService.getOrderByNumber(orderNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/search")
    public ResponseEntity<List<Order>> searchOrders(@RequestBody OrderSearchDTO searchDTO) {
        List<Order> orders = orderService.searchOrders(searchDTO);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/sync")
    public ResponseEntity<?> updateSyncStatus(@PathVariable Long orderId, @RequestParam Boolean synced) {
        try {
            Order order = orderService.updateSyncStatus(orderId, synced);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/unsynced")
    public ResponseEntity<List<Order>> getUnsyncedOrders() {
        return ResponseEntity.ok(orderService.getUnsyncedOrders());
    }

    @GetMapping("/{orderId}/json")
    public ResponseEntity<?> getOrderJson(@PathVariable Long orderId) {
        try {
            return orderService.getOrderByNumber(orderId.toString())
                    .map(order -> ResponseEntity.ok(order.getOrderJson()))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
