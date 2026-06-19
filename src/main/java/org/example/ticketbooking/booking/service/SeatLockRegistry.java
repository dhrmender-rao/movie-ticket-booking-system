package org.example.ticketbooking.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fast-fail lock registry for seat holds.
 *
 * Purpose: prevent 999 threads from queuing on the DB row lock when 1000 concurrent
 * requests target the same seat. Requests are rejected here in microseconds without
 * touching a DB connection.
 *
 * Design:
 *  - ConcurrentHashMap<seatId, holdExpiresAt> is the registry.
 *  - tryLockAll() atomically claims every requested seat or rolls back all claims ("all or nothing").
 *  - The DB pessimistic lock (SELECT FOR UPDATE) remains the authoritative safety net.
 *    This layer only stops the stampede; correctness is still guaranteed by the DB.
 *
 * Limitation: single JVM only. In a multi-instance deployment, replace with Redis SETNX.
 */
@Component
@Slf4j
public class SeatLockRegistry {

    // seatId → expiry timestamp of the in-memory hold
    private final ConcurrentHashMap<Long, LocalDateTime> locks = new ConcurrentHashMap<>();

    /**
     * Attempts to claim all seats atomically.
     *
     * Uses ConcurrentHashMap.compute() which is atomic per key (no external synchronization needed).
     * "All or nothing": if any seat is already actively held, every seat claimed so far is released
     * and the method returns false.
     *
     * @return true if all seats were claimed, false if any seat was already held
     */
    public boolean tryLockAll(List<Long> seatIds, LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        List<Long> acquired = new ArrayList<>();

        for (Long seatId : seatIds) {
            boolean[] claimed = {false};

            locks.compute(seatId, (id, current) -> {
                // Claim if: no entry, or entry has already expired
                if (current == null || current.isBefore(now)) {
                    claimed[0] = true;
                    return expiresAt;
                }
                return current; // actively held — leave it unchanged
            });

            if (claimed[0]) {
                acquired.add(seatId);
            } else {
                // This seat is actively held — roll back everything we claimed so far
                acquired.forEach(locks::remove);
                log.debug("Fast-fail: seat {} already held in memory, released {} previously acquired seats",
                        seatId, acquired.size());
                return false;
            }
        }

        return true;
    }

    /**
     * Releases in-memory locks for the given seats.
     * Called on booking confirmation, cancellation, or any error path.
     */
    public void releaseAll(List<Long> seatIds) {
        seatIds.forEach(locks::remove);
    }

    /**
     * Removes entries whose TTL has passed.
     * Called by HoldExpiryScheduler alongside the DB cleanup — keeps the map from growing unbounded.
     */
    public void evictExpired() {
        LocalDateTime now = LocalDateTime.now();
        int before = locks.size();
        locks.entrySet().removeIf(e -> e.getValue().isBefore(now));
        int evicted = before - locks.size();
        if (evicted > 0) {
            log.debug("Evicted {} expired in-memory seat locks", evicted);
        }
    }
}
