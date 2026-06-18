package org.example.ticketbooking.booking.structs.response;

import org.example.ticketbooking.booking.entity.DiscountCode;
import org.example.ticketbooking.booking.structs.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DiscountCodeResponse(Long id, String code, DiscountType discountType, BigDecimal value,
                                   int maxUses, int usedCount, LocalDateTime validFrom,
                                   LocalDateTime validTo, boolean active) {
    public static DiscountCodeResponse from(DiscountCode d) {
        return new DiscountCodeResponse(d.getId(), d.getCode(), d.getDiscountType(), d.getValue(),
                d.getMaxUses(), d.getUsedCount(), d.getValidFrom(), d.getValidTo(), d.isActive());
    }
}
