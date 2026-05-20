package org.assignments.order.outbox.repository;

import jakarta.persistence.LockModeType;
import org.assignments.order.outbox.entity.OutboxMessage;
import org.assignments.order.enums.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    /**
     * Fetch the oldest PENDING messages up to a batch limit.
     * SKIP LOCKED ensures multiple poller instances don't double-process rows.
     * (H2 doesn't support SKIP LOCKED — use PESSIMISTIC_WRITE for local dev.)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           SELECT m FROM OutboxMessage m
           WHERE m.status = 'PENDING'
             AND m.retryCount < m.maxRetries
           ORDER BY m.createdAt ASC
           """)
    List<OutboxMessage> findPendingForDispatch(Pageable pageable);

    /**
     * Dead messages — PENDING but exhausted retries (safety net query).
     */
    @Query("""
           SELECT m FROM OutboxMessage m
           WHERE m.status = 'FAILED'
             AND m.createdAt >= :since
           ORDER BY m.createdAt DESC
           """)
    List<OutboxMessage> findRecentlyFailed(@Param("since") LocalDateTime since);

    /** Metrics: count by status. */
    long countByStatus(OutboxStatus status);

    /** Cleanup — delete published messages older than a given time. */
    @Modifying
    @Query("DELETE FROM OutboxMessage m WHERE m.status = 'PUBLISHED' AND m.publishedAt < :before")
    int deletePublishedBefore(@Param("before") LocalDateTime before);

    List<OutboxMessage> findByCorrelationId(String correlationId);

    List<OutboxMessage> findByAggregateId(String aggregateId);
}
