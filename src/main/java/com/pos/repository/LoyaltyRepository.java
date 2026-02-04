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
    
    @Query("SELECT l FROM Loyalty l WHERE l.active = true AND " +
           "(l.productBarcode = ?1 OR l.category = ?2 OR (l.productBarcode IS NULL AND l.category IS NULL)) AND " +
           "(l.startDate IS NULL OR l.startDate <= ?3) AND " +
           "(l.endDate IS NULL OR l.endDate >= ?3)")
    List<Loyalty> findApplicableLoyalties(String barcode, String category, LocalDateTime now);
}
