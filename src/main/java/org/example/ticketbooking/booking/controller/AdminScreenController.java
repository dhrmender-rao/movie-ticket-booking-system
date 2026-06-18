package org.example.ticketbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.request.CreateScreenRequest;
import org.example.ticketbooking.booking.service.ScreenService;
import org.example.ticketbooking.booking.structs.response.ScreenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/screens")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminScreenController {

    private final ScreenService screenService;

    @PostMapping
    public ResponseEntity<ScreenResponse> createScreen(@Valid @RequestBody CreateScreenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(screenService.createScreen(request));
    }
}
