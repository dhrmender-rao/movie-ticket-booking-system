package org.example.ticketbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.request.CreatePricingTierRequest;
import org.example.ticketbooking.booking.structs.response.PricingTierResponse;
import org.example.ticketbooking.booking.entity.PricingTier;
import org.example.ticketbooking.booking.entity.Show;
import org.example.ticketbooking.booking.structs.enums.DayType;
import org.example.ticketbooking.booking.structs.enums.SeatType;
import org.example.ticketbooking.common.exception.AppException;
import org.example.ticketbooking.booking.repository.PricingTierRepository;
import org.example.ticketbooking.booking.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingTierRepository pricingTierRepository;
    private final ShowRepository showRepository;

    @Transactional
    public PricingTierResponse createPricingTier(CreatePricingTierRequest request) {
        Show show = showRepository.findById(request.showId())
                .orElseThrow(() -> AppException.notFound("Show", request.showId()));

        // Check for duplicate
        if (pricingTierRepository.findByShowIdAndSeatTypeAndDayType(
                request.showId(), request.seatType(), request.dayType()).isPresent()) {
            throw AppException.conflict("PRICING_EXISTS",
                    "Pricing already configured for this show/seatType/dayType combination");
        }

        PricingTier tier = PricingTier.builder()
                .show(show)
                .seatType(request.seatType())
                .dayType(request.dayType())
                .basePrice(request.basePrice())
                .build();

        return PricingTierResponse.from(pricingTierRepository.save(tier));
    }

    public List<PricingTierResponse> getPricingForShow(Long showId) {
        return pricingTierRepository.findByShowId(showId).stream()
                .map(PricingTierResponse::from).toList();
    }

    public BigDecimal getPrice(Long showId, SeatType seatType, LocalDateTime showTime) {
        DayType dayType = isDayWeekend(showTime) ? DayType.WEEKEND : DayType.WEEKDAY;
        return pricingTierRepository.findByShowIdAndSeatTypeAndDayType(showId, seatType, dayType)
                .map(PricingTier::getBasePrice)
                .orElseThrow(() -> AppException.badRequest(
                        "No pricing configured for seatType=" + seatType + " dayType=" + dayType));
    }

    public static DayType getDayType(LocalDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        return (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) ? DayType.WEEKEND : DayType.WEEKDAY;
    }

    private boolean isDayWeekend(LocalDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
