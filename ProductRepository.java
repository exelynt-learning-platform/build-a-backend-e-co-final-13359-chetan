package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStockQuantityGreaterThan(int quantity);

    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0")
    List<Product> findAllAvailable();
}
