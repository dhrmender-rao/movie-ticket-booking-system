package org.example.ticketbooking.booking.structs.response;

import org.example.ticketbooking.booking.entity.RefundPolicy;

import java.math.BigDecimal;

public record RefundPolicyResponse(Long id, int hoursBeforeShow, BigDecimal refundPercentage, String description) {
    public static RefundPolicyResponse from(RefundPolicy r) {
        return new RefundPolicyResponse(r.getId(), r.getHoursBeforeShow(),
                r.getRefundPercentage(), r.getDescription());
    }
}
