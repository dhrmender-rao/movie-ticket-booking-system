package org.example.ticketbooking.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketbooking.booking.structs.internal.DiscountApplicationResult;
import org.example.ticketbooking.booking.structs.response.BookingResponse;
import org.example.ticketbooking.booking.structs.response.PaymentResponse;
import org.example.ticketbooking.booking.entity.Booking;
import org.example.ticketbooking.booking.entity.BookingItem;
import org.example.ticketbooking.booking.entity.DiscountCode;
import org.example.ticketbooking.booking.entity.Payment;
import org.example.ticketbooking.booking.entity.Show;
import org.example.ticketbooking.booking.entity.ShowSeat;
import org.example.ticketbooking.authanduser.entity.User;
import org.example.ticketbooking.booking.structs.enums.BookingStatus;
import org.example.ticketbooking.booking.structs.enums.PaymentStatus;
import org.example.ticketbooking.booking.structs.enums.ShowSeatStatus;
import org.example.ticketbooking.booking.structs.enums.ShowStatus;
import org.example.ticketbooking.common.exception.AppException;
import org.example.ticketbooking.booking.repository.BookingItemRepository;
import org.example.ticketbooking.booking.repository.BookingRepository;
import org.example.ticketbooking.booking.repository.DiscountCodeRepository;
import org.example.ticketbooking.booking.repository.PaymentRepository;
import org.example.ticketbooking.booking.repository.ShowRepository;
import org.example.ticketbooking.booking.repository.ShowSeatRepository;
import org.example.ticketbooking.authanduser.repository.UserRepository;
import org.example.ticketbooking.booking.structs.request.CreateBookingRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountService discountService;
    private final PricingService pricingService;
    private final RefundPolicyService refundPolicyService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> AppException.notFound("User", 0L));

        Show show = showRepository.findById(request.showId())
                .orElseThrow(() -> AppException.notFound("Show", request.showId()));

        if (show.getStatus() == ShowStatus.CANCELLED) {
            throw AppException.badRequest("Cannot book tickets for a cancelled show");
        }

        // Validate and lock seats
        List<ShowSeat> showSeats = showSeatRepository.findAllByIdWithLock(request.showSeatIds());

        if (showSeats.size() != request.showSeatIds().size()) {
            throw AppException.badRequest("One or more seat IDs are invalid");
        }

        LocalDateTime now = LocalDateTime.now();
        for (ShowSeat ss : showSeats) {
            if (!ss.getShow().getId().equals(request.showId())) {
                throw AppException.badRequest("Seat " + ss.getId() + " does not belong to show " + request.showId());
            }
            if (ss.getStatus() != ShowSeatStatus.HELD) {
                throw AppException.conflict("SEAT_NOT_HELD",
                        "Seat " + ss.getSeat().getRowLabel() + ss.getSeat().getColNumber() + " is not on hold");
            }
            if (ss.getHeldByUser() == null || !ss.getHeldByUser().getId().equals(user.getId())) {
                throw AppException.conflict("SEAT_HELD_BY_OTHER",
                        "Seat " + ss.getSeat().getRowLabel() + ss.getSeat().getColNumber() + " is held by another user");
            }
            if (ss.getHoldExpiresAt() != null && ss.getHoldExpiresAt().isBefore(now)) {
                throw AppException.conflict("HOLD_EXPIRED",
                        "Hold on seat " + ss.getSeat().getRowLabel() + ss.getSeat().getColNumber() + " has expired");
            }
        }

        // Calculate pricing
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<BigDecimal> seatPrices = new ArrayList<>();
        for (ShowSeat ss : showSeats) {
            BigDecimal price = pricingService.getPrice(
                    show.getId(), ss.getSeat().getSeatType(), show.getStartTime());
            seatPrices.add(price);
            totalAmount = totalAmount.add(price);
        }

        // Apply discount — validates, calculates, and increments usage atomically
        DiscountApplicationResult discountResult = discountService.applyDiscount(request.discountCode(), totalAmount);
        BigDecimal discountAmount = discountResult.discountAmount();
        BigDecimal finalAmount = totalAmount.subtract(discountAmount).max(BigDecimal.ZERO);

        // Load discount code entity locally for FK wire-up (stays within this service)
        DiscountCode discountCodeEntity = discountResult.hasDiscount()
                ? discountCodeRepository.findById(discountResult.discountCodeId()).orElseThrow()
                : null;

        // Create booking
        Booking booking = Booking.builder()
                .user(user)
                .show(show)
                .discountCode(discountCodeEntity)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .status(BookingStatus.PENDING)
                .build();
        booking = bookingRepository.save(booking);

        // Create booking items and mark seats as BOOKED
        List<BookingItem> items = new ArrayList<>();
        for (int i = 0; i < showSeats.size(); i++) {
            ShowSeat ss = showSeats.get(i);
            ss.setStatus(ShowSeatStatus.BOOKED);
            ss.setHoldExpiresAt(null);
            ss.setHeldByUser(null);
            showSeatRepository.save(ss);

            items.add(BookingItem.builder()
                    .booking(booking)
                    .showSeat(ss)
                    .priceAtBooking(seatPrices.get(i))
                    .build());
        }
        List<BookingItem> savedItems = bookingItemRepository.saveAll(items);
        booking.setItems(savedItems);

        // Create pending payment
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(finalAmount)
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        log.info("Booking created: id={}, user={}, show={}, seats={}, total={}",
                booking.getId(), userEmail, show.getId(), showSeats.size(), finalAmount);

        return BookingResponse.from(booking, PaymentResponse.from(payment));
    }

    public List<BookingResponse> getMyBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> AppException.notFound("User", 0L));
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(b -> {
                    Payment payment = paymentRepository.findByBookingId(b.getId()).orElse(null);
                    return BookingResponse.from(b, PaymentResponse.from(payment));
                }).toList();
    }

    public BookingResponse getBooking(Long bookingId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> AppException.notFound("User", 0L));

        Booking booking = bookingRepository.findByIdWithFullDetails(bookingId)
                .orElseThrow(() -> AppException.notFound("Booking", bookingId));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw AppException.forbidden("You are not authorized to view this booking");
        }

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        return BookingResponse.from(booking, PaymentResponse.from(payment));
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> AppException.notFound("User", 0L));

        Booking booking = bookingRepository.findByIdWithItems(bookingId)
                .orElseThrow(() -> AppException.notFound("Booking", bookingId));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw AppException.forbidden("You are not authorized to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw AppException.badRequest("Booking is already cancelled");
        }

        if (booking.getShow().getStartTime().isBefore(LocalDateTime.now())) {
            throw AppException.badRequest("Cannot cancel a booking after the show has started");
        }

        // Calculate refund
        BigDecimal refundAmount = refundPolicyService.calculateRefund(
                booking.getShow().getStartTime(), booking.getFinalAmount());

        // Process refund if payment was successful
        paymentService.processRefund(booking.getId(), refundAmount);

        // Release seats
        if (booking.getItems() != null) {
            for (BookingItem item : booking.getItems()) {
                ShowSeat ss = item.getShowSeat();
                ss.setStatus(ShowSeatStatus.AVAILABLE);
                showSeatRepository.save(ss);
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        String refundInfo = refundAmount.compareTo(BigDecimal.ZERO) > 0
                ? "Refund of ₹" + refundAmount + " has been initiated."
                : "No refund applicable per current policy.";
        notificationService.sendCancellationNotification(booking.getId(), refundInfo);

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        return BookingResponse.from(booking, PaymentResponse.from(payment));
    }
}
