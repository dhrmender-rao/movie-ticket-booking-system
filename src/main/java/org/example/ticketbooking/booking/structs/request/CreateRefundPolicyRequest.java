package org.example.ticketbooking.booking.structs.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateRefundPolicyRequest(
        @Min(0) int hoursBeforeShow,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal refundPercentage,
        String description
) {}
