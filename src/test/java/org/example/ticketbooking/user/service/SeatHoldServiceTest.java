package org.example.ticketbooking.user.service;

import org.example.ticketbooking.authanduser.entity.User;
import org.example.ticketbooking.authanduser.structs.enums.Role;
import org.example.ticketbooking.booking.entity.*;
import org.example.ticketbooking.booking.service.SeatHoldService;
import org.example.ticketbooking.booking.structs.enums.SeatType;
import org.example.ticketbooking.booking.structs.enums.ShowSeatStatus;
import org.example.ticketbooking.booking.structs.enums.ShowStatus;
import org.example.ticketbooking.common.exception.AppException;
import org.example.ticketbooking.booking.repository.ShowSeatRepository;
import org.example.ticketbooking.authanduser.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatHoldServiceTest {

    @Mock ShowSeatRepository showSeatRepository;
    @Mock UserRepository userRepository;

    @InjectMocks
    SeatHoldService seatHoldService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seatHoldService, "holdTtlMinutes", 10);
    }

    private User testUser() {
        return User.builder().id(1L).email("alice@test.com").role(Role.CUSTOMER).build();
    }

    private Show testShow(Long id) {
        return Show.builder().id(id).status(ShowStatus.ACTIVE)
                .movie(Movie.builder().title("Test Movie").build())
                .screen(Screen.builder().theater(
                        Theater.builder().city(City.builder().name("Delhi").build()).build()).build())
                .startTime(LocalDateTime.now().plusDays(2))
                .build();
    }

    private ShowSeat availableSeat(Long id, Long showId) {
        return ShowSeat.builder()
                .id(id)
                .show(testShow(showId))
                .seat(Seat.builder().id(id).rowLabel("A").colNumber(id.intValue()).seatType(SeatType.REGULAR).build())
                .status(ShowSeatStatus.AVAILABLE)
                .build();
    }

    @Test
    void holdSeats_success() {
        User user = testUser();
        ShowSeat seat1 = availableSeat(1L, 10L);
        ShowSeat seat2 = availableSeat(2L, 10L);

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(showSeatRepository.findAllByIdWithLock(List.of(1L, 2L))).thenReturn(List.of(seat1, seat2));
        when(showSeatRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = seatHoldService.holdSeats(10L, List.of(1L, 2L), "alice@test.com");

        assertThat(response.seats()).hasSize(2);
        assertThat(response.expiresAt()).isAfter(LocalDateTime.now());
        assertThat(seat1.getStatus()).isEqualTo(ShowSeatStatus.HELD);
        assertThat(seat2.getStatus()).isEqualTo(ShowSeatStatus.HELD);
    }

    @Test
    void holdSeats_alreadyBooked_throws409() {
        User user = testUser();
        ShowSeat seat = availableSeat(1L, 10L);
        seat.setStatus(ShowSeatStatus.BOOKED);

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(showSeatRepository.findAllByIdWithLock(List.of(1L))).thenReturn(List.of(seat));

        assertThatThrownBy(() -> seatHoldService.holdSeats(10L, List.of(1L), "alice@test.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    void holdSeats_alreadyHeld_throws409() {
        User user = testUser();
        ShowSeat seat = availableSeat(1L, 10L);
        seat.setStatus(ShowSeatStatus.HELD);
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5)); // not expired

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(showSeatRepository.findAllByIdWithLock(List.of(1L))).thenReturn(List.of(seat));

        assertThatThrownBy(() -> seatHoldService.holdSeats(10L, List.of(1L), "alice@test.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("already held");
    }

    @Test
    void holdSeats_expiredHold_canBeReHeld() {
        User user = testUser();
        ShowSeat seat = availableSeat(1L, 10L);
        seat.setStatus(ShowSeatStatus.HELD);
        seat.setHoldExpiresAt(LocalDateTime.now().minusMinutes(1)); // expired

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(showSeatRepository.findAllByIdWithLock(List.of(1L))).thenReturn(List.of(seat));
        when(showSeatRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = seatHoldService.holdSeats(10L, List.of(1L), "alice@test.com");

        assertThat(response.seats()).hasSize(1);
        assertThat(seat.getStatus()).isEqualTo(ShowSeatStatus.HELD);
    }

    @Test
    void holdSeats_wrongShow_throws() {
        User user = testUser();
        ShowSeat seat = availableSeat(1L, 99L); // belongs to show 99, not 10

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(showSeatRepository.findAllByIdWithLock(List.of(1L))).thenReturn(List.of(seat));

        assertThatThrownBy(() -> seatHoldService.holdSeats(10L, List.of(1L), "alice@test.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("does not belong to show");
    }
}
