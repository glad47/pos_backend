package com.pos.repository;

import com.pos.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByBarcode(String barcode);
    List<Product> findByActiveTrue();
    List<Product> findByCategory(String category);
    boolean existsByBarcode(String barcode);
}
