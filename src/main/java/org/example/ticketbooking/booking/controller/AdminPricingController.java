package org.example.ticketbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.request.CreatePricingTierRequest;
import org.example.ticketbooking.booking.service.PricingService;
import org.example.ticketbooking.booking.structs.response.PricingTierResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pricing")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPricingController {

    private final PricingService pricingService;

    @PostMapping
    public ResponseEntity<PricingTierResponse> createPricingTier(
            @Valid @RequestBody CreatePricingTierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingService.createPricingTier(request));
    }

    @GetMapping("/show/{showId}")
    public ResponseEntity<List<PricingTierResponse>> getPricingForShow(@PathVariable Long showId) {
        return ResponseEntity.ok(pricingService.getPricingForShow(showId));
    }
}
