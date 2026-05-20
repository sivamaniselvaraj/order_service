package org.assignments.order.outbox.controller;

import lombok.RequiredArgsConstructor;
import org.assignments.order.outbox.entity.OutboxMessage;
import org.assignments.order.outbox.service.OutboxMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for outbox visibility and remediation.
 *
 * In production, secure these behind an internal role (e.g. ROLE_ADMIN).
 */
@RestController
@RequestMapping("/api/v1/outbox")
@RequiredArgsConstructor
public class OutboxController {

    @Autowired
    OutboxMonitorService monitorService;

    /** GET /api/v1/outbox/stats — PENDING / PUBLISHED / FAILED counts */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(monitorService.getStatusCounts());
    }

    /** GET /api/v1/outbox/failed?hours=24 — recently failed messages */
    @GetMapping("/failed")
    public ResponseEntity<List<OutboxMessage>> recentlyFailed(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(monitorService.getRecentlyFailed(hours));
    }

    /** GET /api/v1/outbox/correlation/{correlationId} — trace all messages for a correlationId */
    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<List<OutboxMessage>> byCorrelation(
            @PathVariable String correlationId) {
        return ResponseEntity.ok(monitorService.getByCorrelationId(correlationId));
    }

    /** GET /api/v1/outbox/order/{orderId} — all outbox messages for an order */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<OutboxMessage>> byOrder(
            @PathVariable String orderId) {
        return ResponseEntity.ok(monitorService.getByOrderId(orderId));
    }

    /** POST /api/v1/outbox/{id}/requeue — manually re-queue a FAILED message */
    @PostMapping("/{id}/requeue")
    public ResponseEntity<OutboxMessage> requeue(@PathVariable Long id) {
        return ResponseEntity.ok(monitorService.requeue(id));
    }
}
