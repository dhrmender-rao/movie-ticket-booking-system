package org.example.ticketbooking.user.service;

import org.example.ticketbooking.booking.service.SeatLockRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SeatLockRegistryTest {

    private SeatLockRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SeatLockRegistry();
    }

    // ─── tryLockAll ─────────────────────────────────────────────────────────────

    @Test
    void tryLockAll_emptyRegistry_claimsAllSeats() {
        boolean result = registry.tryLockAll(List.of(1L, 2L, 3L), future());
        assertThat(result).isTrue();
    }

    @Test
    void tryLockAll_seatsActivelyHeld_returnsFalse() {
        registry.tryLockAll(List.of(1L), future()); // seat 1 is now held

        boolean result = registry.tryLockAll(List.of(1L), future());

        assertThat(result).isFalse();
    }

    @Test
    void tryLockAll_holdExpired_canReclaimSeat() {
        // Seat 1 held with already-past expiry
        registry.tryLockAll(List.of(1L), LocalDateTime.now().minusSeconds(1));

        // Should be reclaimable because the entry has expired
        boolean result = registry.tryLockAll(List.of(1L), future());

        assertThat(result).isTrue();
    }

    @Test
    void tryLockAll_allOrNothing_rollsBackIfAnyFails() {
        registry.tryLockAll(List.of(2L), future()); // seat 2 is already held

        // Request [1, 2]: seat 1 would be acquired first, then seat 2 fails
        boolean result = registry.tryLockAll(List.of(1L, 2L), future());

        assertThat(result).isFalse();

        // Seat 1 must have been rolled back — a new request for seat 1 alone should succeed
        boolean seat1Available = registry.tryLockAll(List.of(1L), future());
        assertThat(seat1Available).isTrue();
    }

    @Test
    void tryLockAll_singleSeat_twoConsecutiveRequests_onlyFirstSucceeds() {
        boolean first = registry.tryLockAll(List.of(5L), future());
        boolean second = registry.tryLockAll(List.of(5L), future());

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    // ─── releaseAll ─────────────────────────────────────────────────────────────

    @Test
    void releaseAll_removesHeldSeats_allowsReHold() {
        registry.tryLockAll(List.of(1L, 2L), future());

        registry.releaseAll(List.of(1L, 2L));

        // Both seats are free again
        boolean result = registry.tryLockAll(List.of(1L, 2L), future());
        assertThat(result).isTrue();
    }

    @Test
    void releaseAll_partialRelease_onlyReleasedSeatsBecomeFree() {
        registry.tryLockAll(List.of(1L, 2L), future());

        registry.releaseAll(List.of(1L)); // release only seat 1

        assertThat(registry.tryLockAll(List.of(1L), future())).isTrue();  // seat 1 free
        assertThat(registry.tryLockAll(List.of(2L), future())).isFalse(); // seat 2 still held
    }

    // ─── evictExpired ────────────────────────────────────────────────────────────

    @Test
    void evictExpired_removesExpiredEntries_keepsActiveOnes() {
        // Seat 1: expired hold
        registry.tryLockAll(List.of(1L), LocalDateTime.now().minusSeconds(1));
        // Seat 2: active hold
        registry.tryLockAll(List.of(2L), future());

        registry.evictExpired();

        // Seat 1 evicted — can be re-held
        assertThat(registry.tryLockAll(List.of(1L), future())).isTrue();
        // Seat 2 still active — cannot be re-held
        assertThat(registry.tryLockAll(List.of(2L), future())).isFalse();
    }

    @Test
    void evictExpired_emptyRegistry_noError() {
        assertThatCode(() -> registry.evictExpired()).doesNotThrowAnyException();
    }

    // ─── helper ─────────────────────────────────────────────────────────────────

    private LocalDateTime future() {
        return LocalDateTime.now().plusMinutes(10);
    }
}
