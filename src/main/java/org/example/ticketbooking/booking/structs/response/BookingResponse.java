package org.example.ticketbooking.booking.structs.response;

import org.example.ticketbooking.booking.entity.Booking;
import org.example.ticketbooking.booking.structs.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(Long id, Long showId, String movieTitle, LocalDateTime showStartTime,
                              String theaterName, String cityName,
                              BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal finalAmount,
                              BookingStatus status, LocalDateTime createdAt,
                              List<BookingItemResponse> items,
                              PaymentResponse payment) {

    public static BookingResponse from(Booking b, PaymentResponse paymentResponse) {
        List<BookingItemResponse> items = b.getItems() != null
                ? b.getItems().stream().map(BookingItemResponse::from).toList()
                : List.of();

        return new BookingResponse(
                b.getId(),
                b.getShow().getId(),
                b.getShow().getMovie().getTitle(),
                b.getShow().getStartTime(),
                b.getShow().getScreen().getTheater().getName(),
                b.getShow().getScreen().getTheater().getCity().getName(),
                b.getTotalAmount(),
                b.getDiscountAmount(),
                b.getFinalAmount(),
                b.getStatus(),
                b.getCreatedAt(),
                items,
                paymentResponse
        );
    }
}
