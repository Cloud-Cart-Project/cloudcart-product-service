package com.cloudcart.product.repository;

import com.cloudcart.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(String category);
    List<Product> findByNameContainingIgnoreCase(String name);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id AND p.stock + :quantity >= 0")
    int updateStockSafely(@Param("id") Long id, @Param("quantity") Integer quantity);
}
