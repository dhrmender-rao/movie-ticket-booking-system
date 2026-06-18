package org.example.ticketbooking.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketbooking.booking.structs.response.HoldResponse;
import org.example.ticketbooking.booking.structs.response.ShowSeatResponse;
import org.example.ticketbooking.booking.entity.ShowSeat;
import org.example.ticketbooking.authanduser.entity.User;
import org.example.ticketbooking.booking.structs.enums.ShowSeatStatus;
import org.example.ticketbooking.common.exception.AppException;
import org.example.ticketbooking.booking.repository.ShowSeatRepository;
import org.example.ticketbooking.authanduser.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatHoldService {

    private final ShowSeatRepository showSeatRepository;
    private final UserRepository userRepository;

    @Value("${app.hold.ttl-minutes:10}")
    private int holdTtlMinutes;

    /**
     * Holds the requested seats for the given user.
     * Uses PESSIMISTIC_WRITE lock to prevent double-holds under concurrent requests.
     */
    @Transactional
    public HoldResponse holdSeats(Long showId, List<Long> showSeatIds, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> AppException.notFound("User", 0L));

        // Acquire pessimistic lock on all seats at once
        List<ShowSeat> showSeats = showSeatRepository.findAllByIdWithLock(showSeatIds);

        if (showSeats.size() != showSeatIds.size()) {
            throw AppException.badRequest("One or more seat IDs are invalid");
        }

        // Validate all belong to the requested show
        for (ShowSeat ss : showSeats) {
            if (!ss.getShow().getId().equals(showId)) {
                throw AppException.badRequest("Seat " + ss.getId() + " does not belong to show " + showId);
            }
        }

        // Check all are AVAILABLE (or HELD by same user with expired hold)
        LocalDateTime now = LocalDateTime.now();
        for (ShowSeat ss : showSeats) {
            boolean isExpiredHold = ss.getStatus() == ShowSeatStatus.HELD
                    && ss.getHoldExpiresAt() != null
                    && ss.getHoldExpiresAt().isBefore(now);

            if (ss.getStatus() == ShowSeatStatus.BOOKED) {
                throw AppException.conflict("SEAT_ALREADY_BOOKED",
                        "Seat " + ss.getSeat().getRowLabel() + ss.getSeat().getColNumber() + " is already booked");
            }
            if (ss.getStatus() == ShowSeatStatus.HELD && !isExpiredHold) {
                throw AppException.conflict("SEAT_ALREADY_HELD",
                        "Seat " + ss.getSeat().getRowLabel() + ss.getSeat().getColNumber() + " is already held");
            }
        }

        LocalDateTime expiresAt = now.plusMinutes(holdTtlMinutes);

        for (ShowSeat ss : showSeats) {
            ss.setStatus(ShowSeatStatus.HELD);
            ss.setHoldExpiresAt(expiresAt);
            ss.setHeldByUser(user);
        }
        showSeatRepository.saveAll(showSeats);

        List<ShowSeatResponse> responses = showSeats.stream().map(ShowSeatResponse::from).toList();
        return new HoldResponse(responses, expiresAt);
    }

    public List<ShowSeatResponse> getSeatsForShow(Long showId) {
        return showSeatRepository.findByShowId(showId).stream()
                .map(ShowSeatResponse::from).toList();
    }
}
