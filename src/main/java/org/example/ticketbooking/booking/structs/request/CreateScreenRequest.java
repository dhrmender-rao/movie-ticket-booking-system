package org.example.ticketbooking.booking.structs.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateScreenRequest(
        @NotBlank String name,
        @NotNull Long theaterId,
        @Min(1) @Max(26) int totalRows,
        @Min(1) @Max(50) int totalCols,
        @Min(1) @Max(26) int premiumRowsFromFront
) {}
