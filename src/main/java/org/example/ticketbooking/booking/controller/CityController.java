package org.example.ticketbooking.booking.controller;

import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.service.CityService;
import org.example.ticketbooking.booking.structs.response.CityResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @GetMapping
    public ResponseEntity<List<CityResponse>> getAllCities() {
        return ResponseEntity.ok(cityService.getAllCities());
    }
}
