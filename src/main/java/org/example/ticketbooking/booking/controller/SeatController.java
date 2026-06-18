package org.example.ticketbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.response.HoldResponse;
import org.example.ticketbooking.booking.structs.response.ShowSeatResponse;
import org.example.ticketbooking.booking.structs.request.HoldSeatsRequest;
import org.example.ticketbooking.booking.service.SeatHoldService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shows/{showId}/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatHoldService seatHoldService;

    @GetMapping
    public ResponseEntity<List<ShowSeatResponse>> getSeats(@PathVariable Long showId) {
        return ResponseEntity.ok(seatHoldService.getSeatsForShow(showId));
    }

    @PostMapping("/hold")
    public ResponseEntity<HoldResponse> holdSeats(
            @PathVariable Long showId,
            @Valid @RequestBody HoldSeatsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(seatHoldService.holdSeats(showId, request.showSeatIds(), userDetails.getUsername()));
    }
}
