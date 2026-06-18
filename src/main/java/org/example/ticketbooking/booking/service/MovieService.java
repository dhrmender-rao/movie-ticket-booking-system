package org.example.ticketbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.request.CreateMovieRequest;
import org.example.ticketbooking.booking.structs.response.MovieResponse;
import org.example.ticketbooking.booking.entity.Movie;
import org.example.ticketbooking.common.exception.AppException;
import org.example.ticketbooking.booking.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    @Transactional
    public MovieResponse createMovie(CreateMovieRequest request) {
        Movie movie = Movie.builder()
                .title(request.title())
                .durationMinutes(request.durationMinutes())
                .language(request.language())
                .genre(request.genre())
                .rating(request.rating())
                .description(request.description())
                .build();
        return MovieResponse.from(movieRepository.save(movie));
    }

    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll().stream().map(MovieResponse::from).toList();
    }

    public MovieResponse getMovie(Long id) {
        return MovieResponse.from(movieRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Movie", id)));
    }

}
