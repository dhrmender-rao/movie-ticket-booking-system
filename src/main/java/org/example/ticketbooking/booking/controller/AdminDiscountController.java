package org.example.ticketbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.request.CreateDiscountCodeRequest;
import org.example.ticketbooking.booking.service.DiscountService;
import org.example.ticketbooking.booking.structs.response.DiscountCodeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/discount-codes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDiscountController {

    private final DiscountService discountService;

    @PostMapping
    public ResponseEntity<DiscountCodeResponse> createDiscountCode(
            @Valid @RequestBody CreateDiscountCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(discountService.createDiscountCode(request));
    }

    @GetMapping
    public ResponseEntity<List<DiscountCodeResponse>> getAllDiscountCodes() {
        return ResponseEntity.ok(discountService.getAllDiscountCodes());
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<DiscountCodeResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(discountService.deactivate(id));
    }
}
