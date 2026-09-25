package com.thundercore.erp.inventory.controller;

import com.thundercore.erp.common.dto.ApiResponse;
import com.thundercore.erp.inventory.entity.Product;
import com.thundercore.erp.inventory.entity.StockMovement;
import com.thundercore.erp.inventory.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/products")
@RequiredArgsConstructor
/**
 * ProductController exposes inventory product APIs for the ERP workspace.
 *
 * <p>Read operations are available to authenticated users. Mutating operations
 * are restricted to managers and administrators because they affect stock
 * health, dashboard analytics, and low-stock notification workflows.</p>
 */
public class ProductController {

    private final ProductService productService;

    @GetMapping
    /** Returns every product for inventory tables and dashboard summaries. */
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.success("Products retrieved", productService.getAllProducts()));
    }

    @GetMapping("/{id}")
    /**
     * Returns a single product by identifier.
     *
     * @param id product primary key
     * @return product wrapped in the shared API envelope
     */
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Product retrieved", productService.getProductById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    /**
     * Creates a product and triggers low-stock and dashboard side effects.
     *
     * @param product validated product payload
     * @return persisted product
     */
    public ResponseEntity<ApiResponse<Product>> createProduct(@Valid @RequestBody Product product) {
        return ResponseEntity.ok(ApiResponse.success("Product created", productService.createProduct(product)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    /**
     * Replaces editable product fields and recalculates stock alert state.
     *
     * @param id product primary key
     * @param product validated product update payload
     * @return updated product
     */
    public ResponseEntity<ApiResponse<Product>> updateProduct(@PathVariable Long id, @Valid @RequestBody Product product) {
        return ResponseEntity.ok(ApiResponse.success("Product updated", productService.updateProduct(id, product)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    /** Deletes a product and broadcasts a dashboard refresh event. */
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }

    @GetMapping("/low-stock")
    /** Returns products whose quantity is at or below reorder threshold. */
    public ResponseEntity<ApiResponse<List<Product>>> getLowStockProducts() {
        return ResponseEntity.ok(ApiResponse.success("Low stock products", productService.getLowStockProducts()));
    }

    @GetMapping("/{id}/stock-movements")
    public ResponseEntity<ApiResponse<List<StockMovement>>> getStockMovements(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Stock movements retrieved", productService.getStockMovements(id)));
    }

    @PostMapping("/{id}/stock-movements")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Product>> createStockMovement(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        StockMovement.MovementType type = StockMovement.MovementType.valueOf(payload.getOrDefault("movementType", "IN").toUpperCase());
        Integer quantity = Integer.valueOf(payload.getOrDefault("quantity", "0"));
        String remarks = payload.getOrDefault("remarks", "Manual stock movement");
        return ResponseEntity.ok(ApiResponse.success("Stock updated", productService.adjustStock(id, type, quantity, remarks)));
    }
}
