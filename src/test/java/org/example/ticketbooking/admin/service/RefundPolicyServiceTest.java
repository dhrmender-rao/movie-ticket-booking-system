package org.example.ticketbooking.admin.service;

import org.example.ticketbooking.booking.entity.RefundPolicy;
import org.example.ticketbooking.booking.repository.RefundPolicyRepository;
import org.example.ticketbooking.booking.service.RefundPolicyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundPolicyServiceTest {

    @Mock RefundPolicyRepository refundPolicyRepository;

    @InjectMocks
    RefundPolicyService refundPolicyService;

    private List<RefundPolicy> standardPolicies() {
        return List.of(
                RefundPolicy.builder().hoursBeforeShow(48).refundPercentage(new BigDecimal("100")).build(),
                RefundPolicy.builder().hoursBeforeShow(24).refundPercentage(new BigDecimal("50")).build(),
                RefundPolicy.builder().hoursBeforeShow(2).refundPercentage(new BigDecimal("25")).build()
        );
    }

    @Test
    void calculateRefund_moreThan48Hours_fullRefund() {
        when(refundPolicyRepository.findAllByOrderByHoursBeforeShowDesc()).thenReturn(standardPolicies());
        // show starts 72 hours from now
        LocalDateTime showTime = LocalDateTime.now().plusHours(72);
        BigDecimal refund = refundPolicyService.calculateRefund(showTime, new BigDecimal("500"));
        assertThat(refund).isEqualByComparingTo(new BigDecimal("500.00")); // 100%
    }

    @Test
    void calculateRefund_between24And48Hours_halfRefund() {
        when(refundPolicyRepository.findAllByOrderByHoursBeforeShowDesc()).thenReturn(standardPolicies());
        LocalDateTime showTime = LocalDateTime.now().plusHours(36);
        BigDecimal refund = refundPolicyService.calculateRefund(showTime, new BigDecimal("500"));
        assertThat(refund).isEqualByComparingTo(new BigDecimal("250.00")); // 50%
    }

    @Test
    void calculateRefund_between2And24Hours_quarterRefund() {
        when(refundPolicyRepository.findAllByOrderByHoursBeforeShowDesc()).thenReturn(standardPolicies());
        LocalDateTime showTime = LocalDateTime.now().plusHours(10);
        BigDecimal refund = refundPolicyService.calculateRefund(showTime, new BigDecimal("500"));
        assertThat(refund).isEqualByComparingTo(new BigDecimal("125.00")); // 25%
    }

    @Test
    void calculateRefund_lessThan2Hours_noRefund() {
        when(refundPolicyRepository.findAllByOrderByHoursBeforeShowDesc()).thenReturn(standardPolicies());
        LocalDateTime showTime = LocalDateTime.now().plusMinutes(30);
        BigDecimal refund = refundPolicyService.calculateRefund(showTime, new BigDecimal("500"));
        assertThat(refund).isEqualByComparingTo(BigDecimal.ZERO); // 0%
    }

    @Test
    void calculateRefund_noPolicies_returnsZero() {
        when(refundPolicyRepository.findAllByOrderByHoursBeforeShowDesc()).thenReturn(List.of());
        LocalDateTime showTime = LocalDateTime.now().plusHours(100);
        BigDecimal refund = refundPolicyService.calculateRefund(showTime, new BigDecimal("500"));
        assertThat(refund).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
