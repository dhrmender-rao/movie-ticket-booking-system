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
    private final SeatLockRegistry seatLockRegistry;

    @Value("${app.hold.ttl-minutes:10}")
    private int holdTtlMinutes;

    /**
     * Holds the requested seats for the given user.
     *
     * Two-layer concurrency protection:
     *  1. In-memory fast-fail (SeatLockRegistry) — rejects concurrent requests immediately,
     *     without consuming a DB connection. 999 of 1000 simultaneous requests for the same
     *     seat are turned away here in microseconds.
     *  2. DB PESSIMISTIC_WRITE (SELECT FOR UPDATE) — authoritative safety net that guarantees
     *     correctness even if the in-memory check is bypassed (e.g. after a JVM restart).
     */
    @Transactional
    public HoldResponse holdSeats(Long showId, List<Long> showSeatIds, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> AppException.notFound("User", 0L));

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(holdTtlMinutes);

        // Layer 1 — in-memory fast-fail: reject without touching the DB
        if (!seatLockRegistry.tryLockAll(showSeatIds, expiresAt)) {
            throw AppException.conflict("SEAT_ALREADY_HELD",
                    "One or more seats are currently being held by another user");
        }

        // Layer 2 — DB pessimistic lock (SELECT FOR UPDATE)
        try {
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

            // Check all are AVAILABLE (or expired HELD — eligible for re-hold)
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

            for (ShowSeat ss : showSeats) {
                ss.setStatus(ShowSeatStatus.HELD);
                ss.setHoldExpiresAt(expiresAt);
                ss.setHeldByUser(user);
            }
            showSeatRepository.saveAll(showSeats);

            List<ShowSeatResponse> responses = showSeats.stream().map(ShowSeatResponse::from).toList();
            return new HoldResponse(responses, expiresAt);

        } catch (Exception e) {
            // Release in-memory locks so the seats are not permanently blocked
            // if the DB layer rejects the hold for any reason.
            seatLockRegistry.releaseAll(showSeatIds);
            throw e;
        }
    }

    public List<ShowSeatResponse> getSeatsForShow(Long showId) {
        return showSeatRepository.findByShowId(showId).stream()
                .map(ShowSeatResponse::from).toList();
    }
}
