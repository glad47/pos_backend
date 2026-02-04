package com.pos.repository;

import com.pos.model.Order;
import com.pos.model.PosSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findBySession(PosSession session);
    List<Order> findBySessionId(Long sessionId);
    List<Order> findByStatus(String status);
}
