package org.example.ticketbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.request.CreateRefundPolicyRequest;
import org.example.ticketbooking.booking.structs.response.RefundPolicyResponse;
import org.example.ticketbooking.booking.entity.RefundPolicy;
import org.example.ticketbooking.common.exception.AppException;
import org.example.ticketbooking.booking.repository.RefundPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundPolicyService {

    private final RefundPolicyRepository refundPolicyRepository;

    @Transactional
    public RefundPolicyResponse createPolicy(CreateRefundPolicyRequest request) {
        RefundPolicy policy = RefundPolicy.builder()
                .hoursBeforeShow(request.hoursBeforeShow())
                .refundPercentage(request.refundPercentage())
                .description(request.description())
                .build();
        return RefundPolicyResponse.from(refundPolicyRepository.save(policy));
    }

    public List<RefundPolicyResponse> getAllPolicies() {
        return refundPolicyRepository.findAllByOrderByHoursBeforeShowDesc()
                .stream().map(RefundPolicyResponse::from).toList();
    }

    @Transactional
    public void deletePolicy(Long id) {
        if (!refundPolicyRepository.existsById(id)) {
            throw AppException.notFound("RefundPolicy", id);
        }
        refundPolicyRepository.deleteById(id);
    }

    /**
     * Returns the refund amount for a given booking based on how far away the show is.
     * Policies are evaluated in descending order of hoursBeforeShow; the first matching
     * policy is used. If no policy matches, refund is 0.
     */
    public BigDecimal calculateRefund(LocalDateTime showStartTime, BigDecimal finalAmount) {
        long hoursUntilShow = ChronoUnit.HOURS.between(LocalDateTime.now(), showStartTime);

        List<RefundPolicy> policies = refundPolicyRepository.findAllByOrderByHoursBeforeShowDesc();

        for (RefundPolicy policy : policies) {
            if (hoursUntilShow >= policy.getHoursBeforeShow()) {
                return finalAmount.multiply(policy.getRefundPercentage())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        }

        return BigDecimal.ZERO;
    }
}
