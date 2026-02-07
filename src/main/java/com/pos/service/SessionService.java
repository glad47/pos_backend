package com.pos.service;

import com.pos.dto.CloseSessionDTO;
import com.pos.dto.OpenSessionDTO;
import com.pos.model.Order;
import com.pos.model.PosSession;
import com.pos.repository.OrderRepository;
import com.pos.repository.PosSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SessionService {

    @Autowired
    private PosSessionRepository sessionRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Open a new session or return existing active session for cashier
     * @param dto OpenSessionDTO with cashier name and opening cash
     * @return PosSession (existing active session or newly created session)
     */
    public PosSession openSession(OpenSessionDTO dto) {
        // Check if cashier already has an active session
        Optional<PosSession> existingSession = sessionRepository.findActiveByCashierName(dto.getCashierName());
        
        if (existingSession.isPresent()) {
            // Return existing active session instead of creating new one
            return existingSession.get();
        }
        
        // No active session found, create a new one
        // Get next session number for this cashier
        Integer maxSessionNumber = sessionRepository.findMaxSessionNumberByCashierName(dto.getCashierName());
        int nextSessionNumber = (maxSessionNumber != null ? maxSessionNumber : 0) + 1;

        PosSession session = new PosSession();
        session.setCashierName(dto.getCashierName());
        session.setSessionNumber(nextSessionNumber);
        session.setOpeningCash(dto.getOpeningCash() != null ? dto.getOpeningCash() : BigDecimal.ZERO);
        session.setStatus(PosSession.SessionStatus.OPEN);
        session.setTotalSales(BigDecimal.ZERO);
        session.setTransactionCount(0);

        return sessionRepository.save(session);
    }

    /**
     * Check if a cashier has an active session
     * @param cashierName Name of the cashier
     * @return true if active session exists, false otherwise
     */
    public boolean hasActiveSession(String cashierName) {
        return sessionRepository.hasActiveSession(cashierName);
    }

    /**
     * Get active session for a cashier
     * @param cashierName Name of the cashier
     * @return Optional<PosSession> containing active session if found
     */
    public Optional<PosSession> getActiveSession(String cashierName) {
        return sessionRepository.findActiveByCashierName(cashierName);
    }

    public PosSession closeSession(Long sessionId, CloseSessionDTO dto) {
        PosSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getStatus() == PosSession.SessionStatus.CLOSED) {
            throw new RuntimeException("Session is already closed");
        }

        // Calculate total sales from orders
        List<Order> orders = orderRepository.findBySessionId(sessionId);
        BigDecimal totalSales = orders.stream()
            .filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED)
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        session.setClosedAt(LocalDateTime.now());
        session.setClosingCash(dto.getClosingCash());
        session.setTotalSales(totalSales);
        session.setTransactionCount(orders.size());
        session.setNotes(dto.getNotes());
        session.setStatus(PosSession.SessionStatus.CLOSED);

        return sessionRepository.save(session);
    }

    public Optional<PosSession> getSessionById(Long id) {
        return sessionRepository.findById(id);
    }

    public List<PosSession> getOpenSessions() {
        return sessionRepository.findByStatus(PosSession.SessionStatus.OPEN);
    }

    public List<PosSession> getAllSessions() {
        return sessionRepository.findAll();
    }
}
