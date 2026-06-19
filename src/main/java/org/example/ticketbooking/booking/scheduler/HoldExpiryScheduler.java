package org.example.ticketbooking.booking.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketbooking.booking.repository.ShowSeatRepository;
import org.example.ticketbooking.booking.service.SeatLockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class HoldExpiryScheduler {

    private final ShowSeatRepository showSeatRepository;
    private final SeatLockRegistry seatLockRegistry;

    /**
     * Runs every 60 seconds.
     * - Releases expired holds in the DB (authoritative state).
     * - Evicts expired entries from the in-memory SeatLockRegistry (keeps the map bounded).
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void releaseExpiredHolds() {
        int released = showSeatRepository.releaseExpiredHolds(LocalDateTime.now());
        if (released > 0) {
            log.info("Released {} expired seat holds", released);
        }
        seatLockRegistry.evictExpired();
    }
}
