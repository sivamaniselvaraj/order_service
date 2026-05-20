package org.assignments.order.outbox.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.assignments.order.enums.OutboxStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


/**
 * Transactional Outbox Message.
 *
 * Written atomically in the same DB transaction as the order.
 * A background poller reads PENDING rows, publishes to Kafka/REST,
 * then marks them PUBLISHED (or FAILED after max retries).
 *
 * This guarantees at-least-once delivery without distributed transactions.
 */
@Entity
@Table(name="outbox_messages", indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status, createdAt"),
        @Index(name = "idx_outbox_correlation",    columnList = "correlationId"),
        @Index(name = "idx_outbox_aggregate",      columnList = "aggregateId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Business key of the originating aggregate (orderId). */
    @Column(nullable = false)
    private String aggregateId;

    /** Type of the aggregate — e.g. "ORDER". */
    @Column(nullable = false)
    private String aggregateType;

    /** Shared correlation ID for end-to-end tracing. */
    @Column(nullable = false)
    private String correlationId;

    /**
     * Logical event name — determines which topic / endpoint to use.
     * e.g. "ORDER_CREATED_PROCESSING", "ORDER_CREATED_NOTIFICATION"
     */
    @Column(nullable = false)
    private String eventType;

    /**
     * Destination:
     * "KAFKA:order.processing", "KAFKA:order.notification",
     */
    @Column(nullable = false)
    private String destination;

    /** JSON-serialised event payload. */
    @Column(nullable = false, columnDefinition = "json")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;


    /** How many publish attempts have been made. */
    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    /** Hard cap — poller stops retrying after this. */
    @Column(nullable = false)
    @Builder.Default
    private int maxRetries = 5;

    /** Timestamp of the last publish attempt. */
    @Column
    private LocalDateTime lastAttemptAt;

    /** Error from the last failed attempt (for debugging). */
    @Column(columnDefinition = "TEXT")
    private String lastError;

    /** When the row was written (same tx as the order). */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** When the row was successfully published. */
    @Column
    private LocalDateTime publishedAt;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    public void markAttempt(String error) {
        this.retryCount++;
        this.lastAttemptAt = LocalDateTime.now();
        this.lastError = error;
        if (retryCount >= maxRetries) {
            this.status = OutboxStatus.FAILED;
        }
    }

    public void markPublished() {
        this.status     = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.lastError  = null;
    }

}