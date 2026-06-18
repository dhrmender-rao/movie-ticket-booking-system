package org.example.ticketbooking.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketbooking.booking.structs.request.CreateShowRequest;
import org.example.ticketbooking.booking.structs.response.ShowResponse;
import org.example.ticketbooking.booking.entity.Booking;
import org.example.ticketbooking.booking.entity.Movie;
import org.example.ticketbooking.booking.entity.Screen;
import org.example.ticketbooking.booking.entity.Seat;
import org.example.ticketbooking.booking.entity.Show;
import org.example.ticketbooking.booking.entity.ShowSeat;
import org.example.ticketbooking.booking.structs.enums.ShowSeatStatus;
import org.example.ticketbooking.booking.structs.enums.ShowStatus;
import org.example.ticketbooking.common.exception.AppException;
import org.example.ticketbooking.booking.repository.BookingRepository;
import org.example.ticketbooking.booking.repository.MovieRepository;
import org.example.ticketbooking.booking.repository.ScreenRepository;
import org.example.ticketbooking.booking.repository.SeatRepository;
import org.example.ticketbooking.booking.repository.ShowRepository;
import org.example.ticketbooking.booking.repository.ShowSeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final RefundPolicyService refundPolicyService;
    private final PaymentService paymentService;

    @Transactional
    public ShowResponse createShow(CreateShowRequest request) {
        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> AppException.notFound("Movie", request.movieId()));
        Screen screen = screenRepository.findById(request.screenId())
                .orElseThrow(() -> AppException.notFound("Screen", request.screenId()));

        LocalDateTime endTime = request.startTime().plusMinutes(movie.getDurationMinutes());

        Show show = Show.builder()
                .movie(movie)
                .screen(screen)
                .startTime(request.startTime())
                .endTime(endTime)
                .status(ShowStatus.ACTIVE)
                .build();

        show = showRepository.save(show);

        // Auto-create ShowSeat entries for every seat in the screen
        List<Seat> seats = seatRepository.findByScreenId(screen.getId());
        final Show savedShow = show;
        List<ShowSeat> showSeats = seats.stream()
                .map(seat -> ShowSeat.builder()
                        .show(savedShow)
                        .seat(seat)
                        .status(ShowSeatStatus.AVAILABLE)
                        .build())
                .collect(Collectors.toList());
        showSeatRepository.saveAll(showSeats);

        return ShowResponse.from(show);
    }

    public List<ShowResponse> getShowsByCity(Long cityId, Long movieId, LocalDate date) {
        List<Show> shows;
        if (movieId != null) {
            shows = showRepository.findByCityIdAndMovieIdAndStatus(
                    cityId, movieId, ShowStatus.ACTIVE,
                    date != null ? date.atStartOfDay() : LocalDateTime.now());
        } else {
            LocalDateTime from = date != null ? date.atStartOfDay() : LocalDateTime.now();
            LocalDateTime to = date != null ? date.plusDays(1).atStartOfDay() : from.plusDays(30);
            shows = showRepository.findByCityIdAndStatusAndStartTimeBetween(cityId, ShowStatus.ACTIVE, from, to);
        }
        return shows.stream().map(ShowResponse::from).toList();
    }

    public ShowResponse getShow(Long id) {
        return ShowResponse.from(showRepository.findByIdWithDetails(id)
                .orElseThrow(() -> AppException.notFound("Show", id)));
    }

    /**
     * Admin cancels a show — triggers 100% refund for all confirmed bookings.
     */
    @Transactional
    public void cancelShow(Long showId) {
        Show show = showRepository.findByIdWithDetails(showId)
                .orElseThrow(() -> AppException.notFound("Show", showId));

        if (show.getStatus() == ShowStatus.CANCELLED) {
            throw AppException.badRequest("Show is already cancelled");
        }

        show.setStatus(ShowStatus.CANCELLED);
        showRepository.save(show);

        // Refund all confirmed bookings at 100%
        List<Booking> confirmedBookings = bookingRepository.findConfirmedBookingsByShowId(showId);
        for (Booking booking : confirmedBookings) {
            try {
                paymentService.processAdminCancelRefund(booking.getId());
                notificationService.sendShowCancellationNotification(booking.getId());
            } catch (Exception e) {
                log.error("Error processing refund for booking {} on show cancellation", booking.getId(), e);
            }
        }

        // Release any held seats
        showSeatRepository.findByShowIdAndStatus(showId, ShowSeatStatus.HELD)
                .forEach(ss -> {
                    ss.setStatus(ShowSeatStatus.AVAILABLE);
                    ss.setHoldExpiresAt(null);
                    ss.setHeldByUser(null);
                });

        log.info("Show {} cancelled. Refunds processed for {} bookings.", showId, confirmedBookings.size());
    }
}
