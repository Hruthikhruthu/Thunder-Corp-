package com.thundercore.erp.sales.repository;

import com.thundercore.erp.sales.entity.SaleOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SaleOrderRepository extends JpaRepository<SaleOrder, Long> {
    long countByStatus(SaleOrder.OrderStatus status);

    @Query("select saleOrder from SaleOrder saleOrder join fetch saleOrder.product")
    List<SaleOrder> findAllWithProduct();

    @Query("select saleOrder from SaleOrder saleOrder join fetch saleOrder.product where saleOrder.id = :id")
    Optional<SaleOrder> findByIdWithProduct(@Param("id") Long id);
}
