package org.example.ticketbooking.booking.structs.response;

import org.example.ticketbooking.booking.entity.ShowSeat;
import org.example.ticketbooking.booking.structs.enums.ShowSeatStatus;
import org.example.ticketbooking.booking.structs.enums.SeatType;

import java.time.LocalDateTime;

public record ShowSeatResponse(Long id, String rowLabel, int colNumber, SeatType seatType,
                               ShowSeatStatus status, LocalDateTime holdExpiresAt) {
    public static ShowSeatResponse from(ShowSeat ss) {
        return new ShowSeatResponse(
                ss.getId(),
                ss.getSeat().getRowLabel(),
                ss.getSeat().getColNumber(),
                ss.getSeat().getSeatType(),
                ss.getStatus(),
                ss.getHoldExpiresAt()
        );
    }
}
