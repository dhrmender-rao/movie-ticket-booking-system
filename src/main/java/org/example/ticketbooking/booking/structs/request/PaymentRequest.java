package org.example.ticketbooking.booking.structs.request;

import jakarta.validation.constraints.NotBlank;

public record PaymentRequest(
        @NotBlank String paymentMethod,
        String cardToken
) {}
