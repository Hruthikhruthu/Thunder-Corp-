package com.thundercore.erp.dashboard.service;

import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
/**
 * DashboardEventService publishes realtime dashboard refreshes over STOMP.
 *
 * <p>
 * Domain services call this after mutations. Subscribers on /topic/dashboard
 * receive the latest aggregate stats plus a reason string identifying what
 * changed.
 * </p>
 */
public class DashboardEventService {

    private final DashboardService dashboardService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Sends a fresh dashboard payload to all connected dashboard clients.
     *
     * @param reason short event code such as product-created or invoice-paid
     */
    public void broadcastDashboardUpdate(String reason) {
        try {
            Map<String, Object> payload = dashboardService.getStats();
            payload.put("reason", reason);
            messagingTemplate.convertAndSend("/topic/dashboard", payload);
        } catch (Exception e) {
            log.warn("Dashboard broadcast failed for reason '{}': {}", reason, e.getMessage());
        }
    }
}
