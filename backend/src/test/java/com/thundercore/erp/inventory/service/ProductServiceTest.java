package com.thundercore.erp.inventory.service;

import com.thundercore.erp.dashboard.service.DashboardEventService;
import com.thundercore.erp.inventory.entity.Product;
import com.thundercore.erp.inventory.repository.ProductRepository;
import com.thundercore.erp.inventory.repository.StockMovementRepository;
import com.thundercore.erp.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private DashboardEventService dashboardEventService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProductBroadcastsDashboardAndNotifiesOnLowStock() {
        Product product = new Product();
        product.setSku("SKU-LOW");
        product.setName("Low Stock Item");
        product.setUnitPrice(BigDecimal.TEN);
        product.setQuantity(2);
        product.setReorderThreshold(5);

        when(productRepository.save(product)).thenReturn(product);

        Product saved = productService.createProduct(product);

        assertThat(saved).isSameAs(product);
        verify(productRepository).save(product);
        verify(dashboardEventService).broadcastDashboardUpdate("product-created");
        verify(notificationService).sendNotificationToRoles(
                eq(Set.of("SUPER_ADMIN", "MANAGER")),
                eq("Low stock alert"),
                contains("Low Stock Item")
        );
    }

    @Test
    void deleteProductRemovesStockMovementsBeforeProduct() {
        Product product = new Product();
        product.setId(42L);
        product.setSku("SKU-DELETE");
        product.setName("Delete Me");

        when(productRepository.findById(42L)).thenReturn(java.util.Optional.of(product));

        productService.deleteProduct(42L);

        verify(stockMovementRepository).deleteByProductId(42L);
        verify(productRepository).delete(product);
        verify(dashboardEventService).broadcastDashboardUpdate("product-deleted");
    }
}
