package org.example.ticketbooking.booking.structs.response;

import org.example.ticketbooking.booking.entity.Payment;
import org.example.ticketbooking.booking.structs.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(Long id, BigDecimal amount, PaymentStatus status,
                              String paymentMethod, String transactionId,
                              LocalDateTime paidAt, BigDecimal refundAmount, LocalDateTime refundedAt) {

    public static PaymentResponse from(Payment p) {
        if (p == null) return null;
        return new PaymentResponse(p.getId(), p.getAmount(), p.getStatus(),
                p.getPaymentMethod(), p.getTransactionId(),
                p.getPaidAt(), p.getRefundAmount(), p.getRefundedAt());
    }
}
