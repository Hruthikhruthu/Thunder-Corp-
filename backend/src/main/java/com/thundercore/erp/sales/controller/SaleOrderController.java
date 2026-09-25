package com.thundercore.erp.sales.controller;

import com.thundercore.erp.common.dto.ApiResponse;
import com.thundercore.erp.sales.entity.SaleOrder;
import com.thundercore.erp.sales.service.SaleOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales/orders")
@RequiredArgsConstructor
public class SaleOrderController {
    private final SaleOrderService saleOrderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SaleOrder>>> getOrders() {
        return ResponseEntity.ok(ApiResponse.success("Sales orders retrieved", saleOrderService.getOrders()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<SaleOrder>> createOrder(@RequestBody SaleOrder order) {
        return ResponseEntity.ok(ApiResponse.success("Sales order created", saleOrderService.createOrder(order)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<SaleOrder>> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        SaleOrder.OrderStatus status = SaleOrder.OrderStatus.valueOf(payload.getOrDefault("status", "DRAFT").toUpperCase());
        return ResponseEntity.ok(ApiResponse.success("Sales order status updated", saleOrderService.updateStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        saleOrderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Sales order deleted", null));
    }
}
