package org.example.ticketbooking.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.request.CreateMovieRequest;
import org.example.ticketbooking.booking.service.MovieService;
import org.example.ticketbooking.booking.structs.response.MovieResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/movies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMovieController {

    private final MovieService movieService;

    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@Valid @RequestBody CreateMovieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movieService.createMovie(request));
    }
}
