package org.example.ticketbooking.booking.structs.internal;

import java.math.BigDecimal;

/**
 * Internal-only DTO. Carries the result of discount validation and calculation
 * across the DiscountService → BookingService boundary.
 * Never exposed via any REST endpoint.
 */
public record DiscountApplicationResult(
        Long discountCodeId,      // null when no code applied
        BigDecimal discountAmount // always non-null; ZERO when no discount
) {
    public boolean hasDiscount() {
        return discountCodeId != null;
    }
}
