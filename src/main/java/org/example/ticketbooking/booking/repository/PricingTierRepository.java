package org.example.ticketbooking.booking.repository;

import org.example.ticketbooking.booking.entity.PricingTier;
import org.example.ticketbooking.booking.structs.enums.DayType;
import org.example.ticketbooking.booking.structs.enums.SeatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingTierRepository extends JpaRepository<PricingTier, Long> {

    List<PricingTier> findByShowId(Long showId);

    Optional<PricingTier> findByShowIdAndSeatTypeAndDayType(Long showId, SeatType seatType, DayType dayType);
}
