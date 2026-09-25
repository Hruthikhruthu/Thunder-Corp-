package com.thundercore.erp.sales.service;

import com.thundercore.erp.dashboard.service.DashboardEventService;
import com.thundercore.erp.inventory.entity.Product;
import com.thundercore.erp.inventory.entity.StockMovement;
import com.thundercore.erp.inventory.repository.ProductRepository;
import com.thundercore.erp.inventory.service.ProductService;
import com.thundercore.erp.sales.entity.SaleOrder;
import com.thundercore.erp.sales.repository.SaleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleOrderService {
    private final SaleOrderRepository saleOrderRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final DashboardEventService dashboardEventService;

    public List<SaleOrder> getOrders() {
        return saleOrderRepository.findAllWithProduct();
    }

    @Transactional
    public SaleOrder createOrder(SaleOrder payload) {
        Product product = productRepository.findById(payload.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        SaleOrder order = new SaleOrder();
        order.setCustomerName(payload.getCustomerName());
        order.setCustomerEmail(payload.getCustomerEmail());
        order.setProduct(product);
        order.setQuantity(payload.getQuantity() == null ? 1 : payload.getQuantity());
        order.setUnitPrice(product.getUnitPrice() == null ? BigDecimal.ZERO : product.getUnitPrice());
        order.setTotalAmount(order.getUnitPrice().multiply(BigDecimal.valueOf(order.getQuantity())));
        order.setStatus(payload.getStatus() == null ? SaleOrder.OrderStatus.DRAFT : payload.getStatus());
        SaleOrder saved = saleOrderRepository.save(order);
        if (saved.getStatus() == SaleOrder.OrderStatus.CONFIRMED || saved.getStatus() == SaleOrder.OrderStatus.FULFILLED) {
            decrementInventory(saved);
        }
        dashboardEventService.broadcastDashboardUpdate("sale-order-created");
        return saved;
    }

    @Transactional
    public SaleOrder updateStatus(Long id, SaleOrder.OrderStatus status) {
        SaleOrder order = saleOrderRepository.findByIdWithProduct(id).orElseThrow(() -> new RuntimeException("Sale order not found"));
        SaleOrder.OrderStatus oldStatus = order.getStatus();
        order.setStatus(status);
        SaleOrder saved = saleOrderRepository.save(order);
        if (!stockAlreadyConsumed(oldStatus) && stockAlreadyConsumed(status)) {
            decrementInventory(saved);
        }
        dashboardEventService.broadcastDashboardUpdate("sale-order-status-updated");
        return saved;
    }

    public void deleteOrder(Long id) {
        saleOrderRepository.deleteById(id);
        dashboardEventService.broadcastDashboardUpdate("sale-order-deleted");
    }

    private boolean stockAlreadyConsumed(SaleOrder.OrderStatus status) {
        return status == SaleOrder.OrderStatus.CONFIRMED || status == SaleOrder.OrderStatus.FULFILLED;
    }

    private void decrementInventory(SaleOrder order) {
        Product product = productRepository.findById(order.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        int available = product.getQuantity() == null ? 0 : product.getQuantity();
        if (available < order.getQuantity()) {
            throw new IllegalArgumentException("Insufficient inventory for sales order");
        }
        product.setQuantity(available - order.getQuantity());
        Product savedProduct = productRepository.save(product);
        productService.recordMovement(savedProduct, StockMovement.MovementType.OUT, order.getQuantity(), "Sales order " + order.getOrderNumber());
    }
}
