package org.example.ticketbooking.booking.structs.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record HoldSeatsRequest(
        @NotNull @NotEmpty List<Long> showSeatIds
) {}
