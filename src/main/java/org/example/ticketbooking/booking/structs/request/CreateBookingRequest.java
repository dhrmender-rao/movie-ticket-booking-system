package org.example.ticketbooking.booking.structs.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateBookingRequest(
        @NotNull Long showId,
        @NotNull @NotEmpty List<Long> showSeatIds,
        String discountCode
) {}
