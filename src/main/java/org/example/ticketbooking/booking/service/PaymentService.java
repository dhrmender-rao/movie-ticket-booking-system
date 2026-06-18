package org.example.ticketbooking.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketbooking.booking.structs.response.BookingResponse;
import org.example.ticketbooking.booking.structs.response.PaymentResponse;
import org.example.ticketbooking.booking.entity.Booking;
import org.example.ticketbooking.booking.entity.Payment;
import org.example.ticketbooking.booking.structs.enums.BookingStatus;
import org.example.ticketbooking.booking.structs.enums.PaymentStatus;
import org.example.ticketbooking.common.exception.AppException;
import org.example.ticketbooking.booking.repository.BookingRepository;
import org.example.ticketbooking.booking.repository.PaymentRepository;
import org.example.ticketbooking.booking.structs.request.PaymentRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    @Value("${app.payment.always-succeed:false}")
    private boolean alwaysSucceed;

    @Transactional
    public BookingResponse processPayment(Long bookingId, PaymentRequest request, String userEmail) {
        Booking booking = bookingRepository.findByIdWithItems(bookingId)
                .orElseThrow(() -> AppException.notFound("Booking", bookingId));

        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw AppException.forbidden("You are not authorized to pay for this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw AppException.badRequest("Booking is not in PENDING state. Current status: " + booking.getStatus());
        }

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> AppException.notFound("Payment for booking", bookingId));

        // Simulate payment processing
        boolean success = alwaysSucceed || new Random().nextInt(10) < 9; // 90% success

        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentMethod(request.paymentMethod());
            payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            log.info("Payment successful for booking {} — txn={}", bookingId, payment.getTransactionId());

            // Async notification — does not block the response
            notificationService.sendBookingConfirmation(booking.getId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("Payment failed for booking {}", bookingId);
            throw new AppException(org.springframework.http.HttpStatus.PAYMENT_REQUIRED,
                    "PAYMENT_FAILED", "Payment processing failed. Please try again.");
        }

        return BookingResponse.from(booking, PaymentResponse.from(payment));
    }

    /**
     * Called during booking cancellation by customer.
     */
    @Transactional
    public void processRefund(Long bookingId, BigDecimal refundAmount) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> AppException.notFound("Payment for booking", bookingId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            payment.setRefundAmount(refundAmount);
            payment.setRefundedAt(LocalDateTime.now());
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
            log.info("Refund of {} processed for booking {}", refundAmount, bookingId);
        }
    }

    /**
     * Called by admin when cancelling a show — always full refund.
     */
    @Transactional
    public void processAdminCancelRefund(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> AppException.notFound("Booking", bookingId));
        processRefund(bookingId, booking.getFinalAmount());
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }
}
