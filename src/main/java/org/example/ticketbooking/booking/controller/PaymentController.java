package org.example.ticketbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.response.BookingResponse;
import org.example.ticketbooking.booking.structs.request.PaymentRequest;
import org.example.ticketbooking.booking.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{bookingId}/pay")
    public ResponseEntity<BookingResponse> pay(
            @PathVariable Long bookingId,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(paymentService.processPayment(bookingId, request, userDetails.getUsername()));
    }
}
