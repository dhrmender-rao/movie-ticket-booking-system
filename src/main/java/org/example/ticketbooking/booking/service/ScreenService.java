package org.example.ticketbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.request.CreateScreenRequest;
import org.example.ticketbooking.booking.structs.response.ScreenResponse;
import org.example.ticketbooking.booking.entity.Screen;
import org.example.ticketbooking.booking.entity.Seat;
import org.example.ticketbooking.booking.entity.Theater;
import org.example.ticketbooking.booking.structs.enums.SeatType;
import org.example.ticketbooking.common.exception.AppException;
import org.example.ticketbooking.booking.repository.ScreenRepository;
import org.example.ticketbooking.booking.repository.SeatRepository;
import org.example.ticketbooking.booking.repository.TheaterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final TheaterRepository theaterRepository;
    private final SeatRepository seatRepository;

    /**
     * Creates a screen and auto-generates seats.
     * Rows are labeled A, B, C... from front.
     * Rows within premiumRowsFromFront are PREMIUM; the rest are REGULAR.
     */
    @Transactional
    public ScreenResponse createScreen(CreateScreenRequest request) {
        Theater theater = theaterRepository.findById(request.theaterId())
                .orElseThrow(() -> AppException.notFound("Theater", request.theaterId()));

        if (request.premiumRowsFromFront() >= request.totalRows()) {
            throw AppException.badRequest("premiumRowsFromFront must be less than totalRows");
        }

        Screen screen = Screen.builder()
                .name(request.name())
                .theater(theater)
                .totalRows(request.totalRows())
                .totalCols(request.totalCols())
                .build();

        screen = screenRepository.save(screen);

        List<Seat> seats = new ArrayList<>();
        for (int row = 0; row < request.totalRows(); row++) {
            String rowLabel = String.valueOf((char) ('A' + row));
            SeatType seatType = row < request.premiumRowsFromFront() ? SeatType.PREMIUM : SeatType.REGULAR;
            for (int col = 1; col <= request.totalCols(); col++) {
                seats.add(Seat.builder()
                        .rowLabel(rowLabel)
                        .colNumber(col)
                        .seatType(seatType)
                        .screen(screen)
                        .build());
            }
        }
        seatRepository.saveAll(seats);

        return ScreenResponse.from(screen);
    }

}
