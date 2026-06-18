package org.example.ticketbooking.booking.structs.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.ticketbooking.booking.structs.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateDiscountCodeRequest(
        @NotBlank String code,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin("0.01") BigDecimal value,
        @Min(1) int maxUses,
        LocalDateTime validFrom,
        LocalDateTime validTo
) {}
