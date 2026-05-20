package org.assignments.order.outbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assignments.order.outbox.entity.OutboxMessage;
import org.assignments.order.enums.OutboxStatus;
import org.assignments.order.outbox.repository.OutboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Provides visibility into outbox health and supports manual remediation
 * (re-queue a FAILED message, dead-letter inspection, etc.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxMonitorService {

    @Autowired
    OutboxRepository outboxRepo;

    /** Live counts of PENDING / PUBLISHED / FAILED rows. */
    @Transactional(readOnly = true)
    public Map<String, Long> getStatusCounts() {
        return Map.of(
                "PENDING",   outboxRepo.countByStatus(OutboxStatus.PENDING),
                "PUBLISHED", outboxRepo.countByStatus(OutboxStatus.PUBLISHED),
                "FAILED",    outboxRepo.countByStatus(OutboxStatus.FAILED)
        );
    }

    /** Messages that failed in the last N hours. */
    @Transactional(readOnly = true)
    public List<OutboxMessage> getRecentlyFailed(int hours) {
        return outboxRepo.findRecentlyFailed(LocalDateTime.now().minusHours(hours));
    }

    /** Messages for a given correlationId (cross-service trace). */
    @Transactional(readOnly = true)
    public List<OutboxMessage> getByCorrelationId(String correlationId) {
        return outboxRepo.findByCorrelationId(correlationId);
    }

    /** Messages for a given orderId. */
    @Transactional(readOnly = true)
    public List<OutboxMessage> getByOrderId(String orderId) {
        return outboxRepo.findByAggregateId(orderId);
    }

    /**
     * Re-queue a FAILED message so the poller retries it.
     * Resets retryCount to 0 and status back to PENDING.
     */
    @Transactional
    public OutboxMessage requeue(Long outboxId) {
        OutboxMessage msg = outboxRepo.findById(outboxId)
                .orElseThrow(() -> new IllegalArgumentException("Outbox message not found: " + outboxId));

        if (msg.getStatus() != OutboxStatus.FAILED) {
            throw new IllegalStateException("Only FAILED messages can be re-queued. Current status: " + msg.getStatus());
        }

        msg.setStatus(OutboxStatus.PENDING);
        msg.setRetryCount(0);
        msg.setLastError("Re-queued manually at " + LocalDateTime.now());
        log.info("[OUTBOX-MONITOR] Re-queued message id={} type={} correlationId={}",
                msg.getId(), msg.getEventType(), msg.getCorrelationId());
        return outboxRepo.save(msg);
    }
}
