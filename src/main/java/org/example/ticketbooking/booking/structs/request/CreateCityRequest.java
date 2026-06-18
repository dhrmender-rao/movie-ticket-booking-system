package org.example.ticketbooking.booking.structs.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCityRequest(
        @NotBlank String name,
        String state,
        String country
) {}
