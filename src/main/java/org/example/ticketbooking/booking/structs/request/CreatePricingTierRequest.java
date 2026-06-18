package org.example.ticketbooking.booking.structs.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.example.ticketbooking.booking.structs.enums.DayType;
import org.example.ticketbooking.booking.structs.enums.SeatType;

import java.math.BigDecimal;

public record CreatePricingTierRequest(
        @NotNull Long showId,
        @NotNull SeatType seatType,
        @NotNull DayType dayType,
        @NotNull @DecimalMin("0.01") BigDecimal basePrice
) {}
