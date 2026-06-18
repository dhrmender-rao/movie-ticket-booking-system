package org.example.ticketbooking.booking.structs.response;

import org.example.ticketbooking.booking.entity.Movie;

public record MovieResponse(Long id, String title, int durationMinutes, String language,
                            String genre, String rating, String description) {
    public static MovieResponse from(Movie m) {
        return new MovieResponse(m.getId(), m.getTitle(), m.getDurationMinutes(),
                m.getLanguage(), m.getGenre(), m.getRating(), m.getDescription());
    }
}
