package org.example.ticketbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.request.CreateShowRequest;
import org.example.ticketbooking.booking.service.ShowService;
import org.example.ticketbooking.booking.structs.response.ShowResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shows")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminShowController {

    private final ShowService showService;

    @PostMapping
    public ResponseEntity<ShowResponse> createShow(@Valid @RequestBody CreateShowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(showService.createShow(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelShow(@PathVariable Long id) {
        showService.cancelShow(id);
        return ResponseEntity.noContent().build();
    }
}
