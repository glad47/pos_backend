package com.pos.repository;

import com.pos.model.Loyalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoyaltyRepository extends JpaRepository<Loyalty, Long> {
    List<Loyalty> findByActiveTrue();

    @Query("SELECT l FROM Loyalty l WHERE l.active = true AND " +
           "(l.startDate IS NULL OR l.startDate <= ?1) AND " +
           "(l.endDate IS NULL OR l.endDate >= ?1)")
    List<Loyalty> findActiveLoyalties(LocalDateTime now);
}
