package org.example.ticketbooking.booking.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketbooking.booking.repository.ShowSeatRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class HoldExpiryScheduler {

    private final ShowSeatRepository showSeatRepository;

    /**
     * Runs every 60 seconds. Releases all seat holds that have passed their expiry time.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void releaseExpiredHolds() {
        int released = showSeatRepository.releaseExpiredHolds(LocalDateTime.now());
        if (released > 0) {
            log.info("Released {} expired seat holds", released);
        }
    }
}
