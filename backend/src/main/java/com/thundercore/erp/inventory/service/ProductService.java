package com.thundercore.erp.inventory.service;

import com.thundercore.erp.dashboard.service.DashboardEventService;
import com.thundercore.erp.inventory.entity.Product;
import com.thundercore.erp.inventory.entity.StockMovement;
import com.thundercore.erp.inventory.repository.ProductRepository;
import com.thundercore.erp.inventory.repository.StockMovementRepository;
import com.thundercore.erp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
/**
 * ProductService handles all inventory-related business logic.
 *
 * <p>Responsibilities include product CRUD, stock threshold evaluation,
 * low-stock notification triggering, and dashboard WebSocket broadcasting after
 * inventory mutations.</p>
 */
public class ProductService {
    
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final DashboardEventService dashboardEventService;
    private final NotificationService notificationService;

    /** Returns the complete product catalog for inventory screens. */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * Loads a product or fails with a domain-friendly error.
     *
     * @param id product primary key
     * @return persisted product
     */
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    /**
     * Persists a product and emits operational side effects.
     *
     * @param product product entity supplied by the controller
     * @return saved product including generated identifier
     */
    public Product createProduct(Product product) {
        Product saved = productRepository.save(product);
        recordMovement(saved, StockMovement.MovementType.IN, saved.getQuantity(), "Opening stock");
        notifyIfLowStock(saved);
        dashboardEventService.broadcastDashboardUpdate("product-created");
        return saved;
    }

    /**
     * Updates product details, then refreshes dashboards and low-stock alerts.
     *
     * @param id product primary key
     * @param productDetails replacement product values
     * @return saved product after update
     */
    public Product updateProduct(Long id, Product productDetails) {
        Product product = getProductById(id);
        product.setName(productDetails.getName());
        product.setSku(productDetails.getSku());
        product.setDescription(productDetails.getDescription());
        product.setCategory(productDetails.getCategory());
        product.setSupplier(productDetails.getSupplier());
        product.setUnitPrice(productDetails.getUnitPrice());
        product.setQuantity(productDetails.getQuantity());
        product.setReorderThreshold(productDetails.getReorderThreshold());
        Product saved = productRepository.save(product);
        notifyIfLowStock(saved);
        dashboardEventService.broadcastDashboardUpdate("product-updated");
        return saved;
    }

    /**
     * Deletes a product and notifies realtime dashboard subscribers.
     *
     * @param id product primary key
     */
    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        stockMovementRepository.deleteByProductId(product.getId());
        productRepository.delete(product);
        dashboardEventService.broadcastDashboardUpdate("product-deleted");
    }

    /** Returns products that require reorder attention. */
    public List<Product> getLowStockProducts() {
        return productRepository.findAll().stream()
            .filter(p -> p.getQuantity() <= p.getReorderThreshold())
            .toList();
    }

    public List<StockMovement> getStockMovements(Long productId) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public Product adjustStock(Long productId, StockMovement.MovementType movementType, Integer quantity, String remarks) {
        Product product = getProductById(productId);
        int current = product.getQuantity() == null ? 0 : product.getQuantity();
        int delta = quantity == null ? 0 : quantity;
        if (delta <= 0) {
            throw new IllegalArgumentException("Stock movement quantity must be greater than zero");
        }
        int nextQuantity = switch (movementType) {
            case IN -> current + delta;
            case OUT -> current - delta;
            case ADJUSTMENT -> delta;
        };
        if (nextQuantity < 0) {
            throw new IllegalArgumentException("Insufficient stock for this movement");
        }
        product.setQuantity(nextQuantity);
        Product saved = productRepository.save(product);
        recordMovement(saved, movementType, delta, remarks);
        notifyIfLowStock(saved);
        dashboardEventService.broadcastDashboardUpdate("stock-movement");
        return saved;
    }

    public void recordMovement(Product product, StockMovement.MovementType movementType, Integer quantity, String remarks) {
        if (product == null || quantity == null || quantity <= 0) {
            return;
        }
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(movementType);
        movement.setQuantity(quantity);
        movement.setRemarks(remarks);
        stockMovementRepository.save(movement);
    }

    /**
     * Creates role-targeted alerts when quantity falls below the threshold.
     *
     * @param product product whose stock level changed
     */
    private void notifyIfLowStock(Product product) {
        int quantity = product.getQuantity() == null ? 0 : product.getQuantity();
        int threshold = product.getReorderThreshold() == null ? 0 : product.getReorderThreshold();
        if (quantity <= threshold) {
            notificationService.sendNotificationToRoles(
                    Set.of("SUPER_ADMIN", "MANAGER"),
                    "Low stock alert",
                    product.getName() + " is at " + quantity + " units. Reorder threshold is " + threshold + "."
            );
        }
    }
}
