package org.example.ticketbooking.booking.structs.response;

import org.example.ticketbooking.booking.entity.PricingTier;
import org.example.ticketbooking.booking.structs.enums.DayType;
import org.example.ticketbooking.booking.structs.enums.SeatType;

import java.math.BigDecimal;

public record PricingTierResponse(Long id, Long showId, SeatType seatType, DayType dayType, BigDecimal basePrice) {
    public static PricingTierResponse from(PricingTier p) {
        return new PricingTierResponse(p.getId(), p.getShow().getId(),
                p.getSeatType(), p.getDayType(), p.getBasePrice());
    }
}
