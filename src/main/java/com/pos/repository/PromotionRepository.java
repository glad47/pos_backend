package com.pos.repository;

import com.pos.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    List<Promotion> findByActiveTrue();
    
    @Query("SELECT p FROM Promotion p WHERE p.active = true AND " +
           "(p.startDate IS NULL OR p.startDate <= ?1) AND " +
           "(p.endDate IS NULL OR p.endDate >= ?1)")
    List<Promotion> findActivePromotions(LocalDateTime now);
    
    @Query("SELECT p FROM Promotion p WHERE p.active = true AND " +
           "(p.productBarcode = ?1 OR p.category = ?2 OR (p.productBarcode IS NULL AND p.category IS NULL)) AND " +
           "(p.startDate IS NULL OR p.startDate <= ?3) AND " +
           "(p.endDate IS NULL OR p.endDate >= ?3)")
    List<Promotion> findApplicablePromotions(String barcode, String category, LocalDateTime now);
}
