package org.example.ticketbooking.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketbooking.booking.entity.Booking;
import org.example.ticketbooking.booking.entity.Notification;
import org.example.ticketbooking.booking.structs.enums.NotificationStatus;
import org.example.ticketbooking.booking.structs.enums.NotificationType;
import org.example.ticketbooking.common.exception.AppException;
import org.example.ticketbooking.booking.repository.BookingRepository;
import org.example.ticketbooking.booking.repository.NotificationRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final BookingRepository bookingRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendBookingConfirmation(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithFullDetails(bookingId)
                .orElseThrow(() -> AppException.notFound("Booking", bookingId));
        String message = buildConfirmationMessage(booking);

        log.info("[EMAIL TRIGGERED] To: {} | Subject: Booking Confirmation — #{} | Body: {}",
                booking.getUser().getEmail(), booking.getId(), message);

        Notification notification = Notification.builder()
                .user(booking.getUser())
                .booking(booking)
                .type(NotificationType.BOOKING_CONFIRMATION)
                .status(NotificationStatus.SENT)
                .message(message)
                .sentAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendShowReminder(Long bookingId) {
        if (notificationRepository.existsByBookingIdAndType(bookingId, NotificationType.SHOW_REMINDER)) {
            return; // already sent
        }
        Booking booking = bookingRepository.findByIdWithFullDetails(bookingId)
                .orElseThrow(() -> AppException.notFound("Booking", bookingId));
        String message = "Reminder: Your show '" + booking.getShow().getMovie().getTitle()
                + "' starts at " + booking.getShow().getStartTime() + ". Enjoy!";

        log.info("[EMAIL TRIGGERED] To: {} | Subject: Show Reminder — {} | Body: {}",
                booking.getUser().getEmail(), booking.getShow().getMovie().getTitle(), message);

        Notification notification = Notification.builder()
                .user(booking.getUser())
                .booking(booking)
                .type(NotificationType.SHOW_REMINDER)
                .status(NotificationStatus.SENT)
                .message(message)
                .sentAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendCancellationNotification(Long bookingId, String refundInfo) {
        Booking booking = bookingRepository.findByIdWithFullDetails(bookingId)
                .orElseThrow(() -> AppException.notFound("Booking", bookingId));
        String message = "Your booking #" + booking.getId() + " for '"
                + booking.getShow().getMovie().getTitle() + "' has been cancelled. " + refundInfo;

        log.info("[EMAIL TRIGGERED] To: {} | Subject: Booking Cancelled — #{} | Body: {}",
                booking.getUser().getEmail(), booking.getId(), message);

        Notification notification = Notification.builder()
                .user(booking.getUser())
                .booking(booking)
                .type(NotificationType.BOOKING_CANCELLATION)
                .status(NotificationStatus.SENT)
                .message(message)
                .sentAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendShowCancellationNotification(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithFullDetails(bookingId)
                .orElseThrow(() -> AppException.notFound("Booking", bookingId));
        String message = "Unfortunately, the show '" + booking.getShow().getMovie().getTitle()
                + "' on " + booking.getShow().getStartTime()
                + " has been cancelled. A full refund has been initiated for booking #" + booking.getId();

        log.info("[EMAIL TRIGGERED] To: {} | Subject: Show Cancelled — {} | Body: {}",
                booking.getUser().getEmail(), booking.getShow().getMovie().getTitle(), message);

        Notification notification = Notification.builder()
                .user(booking.getUser())
                .booking(booking)
                .type(NotificationType.SHOW_CANCELLATION)
                .status(NotificationStatus.SENT)
                .message(message)
                .sentAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    private String buildConfirmationMessage(Booking booking) {
        return String.format(
                "Booking confirmed! #%d — %s at %s | %s | Seats: %d | Total: ₹%.2f",
                booking.getId(),
                booking.getShow().getMovie().getTitle(),
                booking.getShow().getStartTime(),
                booking.getShow().getScreen().getTheater().getName(),
                booking.getItems() != null ? booking.getItems().size() : 0,
                booking.getFinalAmount()
        );
    }
}
