package org.example.ticketbooking.booking.controller;

import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.service.ShowService;
import org.example.ticketbooking.booking.structs.response.ShowResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @GetMapping("/cities/{cityId}/shows")
    public ResponseEntity<List<ShowResponse>> getShowsByCity(
            @PathVariable Long cityId,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(showService.getShowsByCity(cityId, movieId, date));
    }

    @GetMapping("/shows/{id}")
    public ResponseEntity<ShowResponse> getShow(@PathVariable Long id) {
        return ResponseEntity.ok(showService.getShow(id));
    }
}
