package com.thundercore.erp.inventory.repository;

import com.thundercore.erp.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * ProductRepository provides catalog queries for inventory, reports, and
 * dashboard aggregation.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
    /** Finds products within a business category for filtering and analytics. */
    List<Product> findByCategory(String category);

    /** Finds products at or below a caller-supplied stock threshold. */
    List<Product> findByQuantityLessThanEqual(Integer threshold);
}
