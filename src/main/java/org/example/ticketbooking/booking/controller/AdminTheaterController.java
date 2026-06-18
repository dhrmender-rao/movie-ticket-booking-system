package org.example.ticketbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.request.CreateTheaterRequest;
import org.example.ticketbooking.booking.service.TheaterService;
import org.example.ticketbooking.booking.structs.response.TheaterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/theaters")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTheaterController {

    private final TheaterService theaterService;

    @PostMapping
    public ResponseEntity<TheaterResponse> createTheater(@Valid @RequestBody CreateTheaterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(theaterService.createTheater(request));
    }
}
