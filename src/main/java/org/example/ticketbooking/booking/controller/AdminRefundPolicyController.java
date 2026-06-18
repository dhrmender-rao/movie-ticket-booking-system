package org.example.ticketbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.request.CreateRefundPolicyRequest;
import org.example.ticketbooking.booking.service.RefundPolicyService;
import org.example.ticketbooking.booking.structs.response.RefundPolicyResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/refund-policies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRefundPolicyController {

    private final RefundPolicyService refundPolicyService;

    @PostMapping
    public ResponseEntity<RefundPolicyResponse> createPolicy(
            @Valid @RequestBody CreateRefundPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(refundPolicyService.createPolicy(request));
    }

    @GetMapping
    public ResponseEntity<List<RefundPolicyResponse>> getAllPolicies() {
        return ResponseEntity.ok(refundPolicyService.getAllPolicies());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        refundPolicyService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }
}
