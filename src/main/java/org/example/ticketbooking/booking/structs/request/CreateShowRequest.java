package org.example.ticketbooking.booking.structs.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateShowRequest(
        @NotNull Long movieId,
        @NotNull Long screenId,
        @NotNull @Future LocalDateTime startTime
) {}
