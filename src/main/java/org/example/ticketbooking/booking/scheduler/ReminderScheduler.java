package org.example.ticketbooking.booking.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketbooking.booking.entity.Booking;
import org.example.ticketbooking.booking.entity.Show;
import org.example.ticketbooking.booking.repository.BookingRepository;
import org.example.ticketbooking.booking.repository.ShowRepository;
import org.example.ticketbooking.booking.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final ShowRepository showRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    /**
     * Runs every 10 minutes.
     * Sends reminders for shows starting ~24 hours from now (within a 10-minute window).
     */
    @Scheduled(fixedDelay = 600_000)
    public void sendShowReminders() {
        LocalDateTime windowStart = LocalDateTime.now().plusHours(24);
        LocalDateTime windowEnd = windowStart.plusMinutes(10);

        List<Show> upcomingShows = showRepository.findActiveShowsStartingBetween(windowStart, windowEnd);

        for (Show show : upcomingShows) {
            List<Booking> bookings = bookingRepository.findConfirmedBookingsByShowId(show.getId());
            for (Booking booking : bookings) {
                try {
                    notificationService.sendShowReminder(booking.getId());
                } catch (Exception e) {
                    log.error("Failed to send reminder for booking {}", booking.getId(), e);
                }
            }
        }

        if (!upcomingShows.isEmpty()) {
            log.info("Reminder check: {} shows with upcoming reminders", upcomingShows.size());
        }
    }
}
