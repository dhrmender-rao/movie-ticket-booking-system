package org.example.ticketbooking.booking.structs.response;

import org.example.ticketbooking.booking.entity.BookingItem;

import java.math.BigDecimal;

public record BookingItemResponse(Long showSeatId, String rowLabel, int colNumber,
                                  String seatType, BigDecimal price) {
    public static BookingItemResponse from(BookingItem item) {
        return new BookingItemResponse(
                item.getShowSeat().getId(),
                item.getShowSeat().getSeat().getRowLabel(),
                item.getShowSeat().getSeat().getColNumber(),
                item.getShowSeat().getSeat().getSeatType().name(),
                item.getPriceAtBooking()
        );
    }
}
