package com.pos.repository;

import com.pos.model.PosSession;
import com.pos.model.PosSession.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PosSessionRepository extends JpaRepository<PosSession, Long> {
    List<PosSession> findByStatus(SessionStatus status);
    Optional<PosSession> findByIdAndStatus(Long id, SessionStatus status);
    List<PosSession> findByCashierName(String cashierName);
    
    @Query("SELECT MAX(s.sessionNumber) FROM PosSession s WHERE s.cashierName = ?1")
    Integer findMaxSessionNumberByCashierName(String cashierName);
}
