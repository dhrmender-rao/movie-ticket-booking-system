package org.example.ticketbooking.admin.service;

import org.example.ticketbooking.booking.structs.internal.DiscountApplicationResult;
import org.example.ticketbooking.booking.entity.DiscountCode;
import org.example.ticketbooking.booking.structs.enums.DiscountType;
import org.example.ticketbooking.common.exception.AppException;
import org.example.ticketbooking.booking.repository.DiscountCodeRepository;
import org.example.ticketbooking.booking.service.DiscountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock DiscountCodeRepository discountCodeRepository;

    @InjectMocks
    DiscountService discountService;

    private DiscountCode validPercentageCode() {
        return DiscountCode.builder()
                .id(1L).code("SAVE20")
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("20"))
                .maxUses(100).usedCount(0)
                .active(true)
                .build();
    }

    private DiscountCode validFlatCode() {
        return DiscountCode.builder()
                .id(2L).code("FLAT50")
                .discountType(DiscountType.FLAT)
                .value(new BigDecimal("50"))
                .maxUses(10).usedCount(0)
                .active(true)
                .build();
    }

    @Test
    void applyDiscount_validPercentageCode_returnsCorrectDiscount() {
        DiscountCode dc = validPercentageCode();
        when(discountCodeRepository.findByCode("SAVE20")).thenReturn(Optional.of(dc));

        DiscountApplicationResult result = discountService.applyDiscount("SAVE20", new BigDecimal("500"));

        assertThat(result.hasDiscount()).isTrue();
        assertThat(result.discountCodeId()).isEqualTo(1L);
        assertThat(result.discountAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        verify(discountCodeRepository).save(dc);
    }

    @Test
    void applyDiscount_validFlatCode_returnsCorrectDiscount() {
        DiscountCode dc = validFlatCode();
        when(discountCodeRepository.findByCode("FLAT50")).thenReturn(Optional.of(dc));

        DiscountApplicationResult result = discountService.applyDiscount("FLAT50", new BigDecimal("200"));

        assertThat(result.hasDiscount()).isTrue();
        assertThat(result.discountCodeId()).isEqualTo(2L);
        assertThat(result.discountAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        verify(discountCodeRepository).save(dc);
    }

    @Test
    void applyDiscount_flatExceedsTotal_cappedAtTotal() {
        DiscountCode dc = validFlatCode(); // flat 50
        when(discountCodeRepository.findByCode("FLAT50")).thenReturn(Optional.of(dc));

        DiscountApplicationResult result = discountService.applyDiscount("FLAT50", new BigDecimal("30"));

        assertThat(result.discountAmount()).isEqualByComparingTo(new BigDecimal("30.00")); // capped at total
        verify(discountCodeRepository).save(dc);
    }

    @Test
    void applyDiscount_nullCode_returnsZeroResult() {
        DiscountApplicationResult result = discountService.applyDiscount(null, new BigDecimal("500"));

        assertThat(result.hasDiscount()).isFalse();
        assertThat(result.discountCodeId()).isNull();
        assertThat(result.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(discountCodeRepository);
    }

    @Test
    void applyDiscount_blankCode_returnsZeroResult() {
        DiscountApplicationResult result = discountService.applyDiscount("  ", new BigDecimal("500"));

        assertThat(result.hasDiscount()).isFalse();
        assertThat(result.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(discountCodeRepository);
    }

    @Test
    void applyDiscount_inactiveCode_throws() {
        DiscountCode dc = validPercentageCode();
        dc.setActive(false);
        when(discountCodeRepository.findByCode("SAVE20")).thenReturn(Optional.of(dc));

        assertThatThrownBy(() -> discountService.applyDiscount("SAVE20", new BigDecimal("500")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void applyDiscount_maxUsesReached_throws() {
        DiscountCode dc = validPercentageCode();
        dc.setUsedCount(100);
        when(discountCodeRepository.findByCode("SAVE20")).thenReturn(Optional.of(dc));

        assertThatThrownBy(() -> discountService.applyDiscount("SAVE20", new BigDecimal("500")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("usage limit");
    }

    @Test
    void applyDiscount_expiredCode_throws() {
        DiscountCode dc = validPercentageCode();
        dc.setValidTo(LocalDateTime.now().minusDays(1));
        when(discountCodeRepository.findByCode("SAVE20")).thenReturn(Optional.of(dc));

        assertThatThrownBy(() -> discountService.applyDiscount("SAVE20", new BigDecimal("500")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void applyDiscount_notYetValidCode_throws() {
        DiscountCode dc = validPercentageCode();
        dc.setValidFrom(LocalDateTime.now().plusDays(1));
        when(discountCodeRepository.findByCode("SAVE20")).thenReturn(Optional.of(dc));

        assertThatThrownBy(() -> discountService.applyDiscount("SAVE20", new BigDecimal("500")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("not yet valid");
    }
}
