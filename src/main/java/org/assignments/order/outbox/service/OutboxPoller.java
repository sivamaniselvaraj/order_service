package org.assignments.order.outbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assignments.order.outbox.dispatcher.OutboxDispatcher;
import org.assignments.order.outbox.entity.OutboxMessage;
import org.assignments.order.outbox.repository.OutboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled poller — runs every {@code outbox.poll.interval-ms} milliseconds.
 *
 * Each tick:
 *  1. Fetches a batch of PENDING outbox rows (PESSIMISTIC_WRITE lock).
 *  2. Attempts to dispatch each row via OutboxDispatcher.
 *  3. Marks the row PUBLISHED on success, increments retryCount on failure.
 *  4. Once retryCount >= maxRetries the row is marked FAILED (dead-letter).
 *
 * The PESSIMISTIC_WRITE lock on the SELECT means multiple service instances
 * won't pick up the same rows (safe for horizontal scaling).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPoller {
    @Autowired
    OutboxRepository outboxRepo;

    @Autowired
    OutboxDispatcher dispatcher;

    // ── Main poll loop ────────────────────────────────────────────────────────

    @Scheduled(fixedDelayString = "${outbox.poll.interval-ms:2000}")
    @Transactional
    public void poll() {
        List<OutboxMessage> batch = outboxRepo.findPendingForDispatch(
                PageRequest.of(0, 50));   // max 50 rows per tick

        if (batch.isEmpty()) return;

        log.debug("[OUTBOX-POLLER] Processing {} pending message(s)", batch.size());

        int ok = 0, fail = 0;
        for (OutboxMessage msg : batch) {
            try {
                dispatcher.dispatch(msg);
                msg.markPublished();
                ok++;
            } catch (Exception ex) {
                msg.markAttempt(truncate(ex.getMessage(), 500));
                log.warn("[OUTBOX-POLLER] Dispatch failed id={} type={} attempt={}/{} error={}",
                        msg.getId(), msg.getEventType(),
                        msg.getRetryCount(), msg.getMaxRetries(),
                        ex.getMessage());
                fail++;
            }
        }

        outboxRepo.saveAll(batch);

        if (ok > 0 || fail > 0) {
            log.info("[OUTBOX-POLLER] Tick complete — published={} failed={}", ok, fail);
        }
    }

    // ── Cleanup job ───────────────────────────────────────────────────────────

    /**
     * Purge PUBLISHED rows older than 7 days to keep the table lean.
     * Runs once per hour.
     */
    @Scheduled(cron = "${outbox.cleanup.cron:0 0 * * * *}")
    @Transactional
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = outboxRepo.deletePublishedBefore(cutoff);
        if (deleted > 0) {
            log.info("[OUTBOX-POLLER] Cleanup — deleted {} published rows older than {}", deleted, cutoff);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}