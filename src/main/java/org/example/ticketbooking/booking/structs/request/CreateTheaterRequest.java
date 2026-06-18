package org.example.ticketbooking.booking.structs.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTheaterRequest(
        @NotBlank String name,
        String address,
        @NotNull Long cityId
) {}
