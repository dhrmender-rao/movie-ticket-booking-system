package org.example.ticketbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.request.CreateDiscountCodeRequest;
import org.example.ticketbooking.booking.structs.internal.DiscountApplicationResult;
import org.example.ticketbooking.booking.structs.response.DiscountCodeResponse;
import org.example.ticketbooking.booking.entity.DiscountCode;
import org.example.ticketbooking.booking.structs.enums.DiscountType;
import org.example.ticketbooking.common.exception.AppException;
import org.example.ticketbooking.booking.repository.DiscountCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountCodeRepository discountCodeRepository;

    @Transactional
    public DiscountCodeResponse createDiscountCode(CreateDiscountCodeRequest request) {
        if (discountCodeRepository.existsByCode(request.code())) {
            throw AppException.conflict("CODE_EXISTS", "Discount code already exists: " + request.code());
        }

        if (request.discountType() == DiscountType.PERCENTAGE &&
                request.value().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw AppException.badRequest("Percentage discount cannot exceed 100%");
        }

        DiscountCode code = DiscountCode.builder()
                .code(request.code().toUpperCase())
                .discountType(request.discountType())
                .value(request.value())
                .maxUses(request.maxUses())
                .usedCount(0)
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .active(true)
                .build();

        return DiscountCodeResponse.from(discountCodeRepository.save(code));
    }

    public List<DiscountCodeResponse> getAllDiscountCodes() {
        return discountCodeRepository.findAll().stream().map(DiscountCodeResponse::from).toList();
    }

    @Transactional
    public DiscountCodeResponse deactivate(Long id) {
        DiscountCode code = discountCodeRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("DiscountCode", id));
        code.setActive(false);
        return DiscountCodeResponse.from(discountCodeRepository.save(code));
    }

    /**
     * Validates the discount code, computes the discount against totalPrice,
     * increments the usage counter atomically, and returns a DiscountApplicationResult.
     * If code is null/blank, returns a zero-discount result without touching the DB.
     */
    @Transactional
    public DiscountApplicationResult applyDiscount(String code, BigDecimal totalPrice) {
        if (code == null || code.isBlank()) {
            return new DiscountApplicationResult(null, BigDecimal.ZERO);
        }

        DiscountCode dc = discountCodeRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> AppException.badRequest("Invalid discount code: " + code));

        if (!dc.isActive()) {
            throw AppException.badRequest("Discount code is inactive");
        }
        if (dc.getUsedCount() >= dc.getMaxUses()) {
            throw AppException.badRequest("Discount code has reached its usage limit");
        }
        LocalDateTime now = LocalDateTime.now();
        if (dc.getValidFrom() != null && now.isBefore(dc.getValidFrom())) {
            throw AppException.badRequest("Discount code is not yet valid");
        }
        if (dc.getValidTo() != null && now.isAfter(dc.getValidTo())) {
            throw AppException.badRequest("Discount code has expired");
        }

        BigDecimal discountAmount = switch (dc.getDiscountType()) {
            case PERCENTAGE -> totalPrice.multiply(dc.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FLAT -> dc.getValue().min(totalPrice);
        };

        dc.setUsedCount(dc.getUsedCount() + 1);
        discountCodeRepository.save(dc);

        return new DiscountApplicationResult(dc.getId(), discountAmount);
    }
}
