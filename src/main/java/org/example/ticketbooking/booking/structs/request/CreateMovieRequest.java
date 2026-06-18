package org.example.ticketbooking.booking.structs.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateMovieRequest(
        @NotBlank String title,
        @Min(1) int durationMinutes,
        String language,
        String genre,
        String rating,
        String description
) {}
