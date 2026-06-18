package org.example.ticketbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.request.CreateCityRequest;
import org.example.ticketbooking.booking.service.CityService;
import org.example.ticketbooking.booking.structs.response.CityResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cities")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCityController {

    private final CityService cityService;

    @PostMapping
    public ResponseEntity<CityResponse> createCity(@Valid @RequestBody CreateCityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cityService.createCity(request));
    }

    @GetMapping
    public ResponseEntity<List<CityResponse>> getAllCities() {
        return ResponseEntity.ok(cityService.getAllCities());
    }
}
