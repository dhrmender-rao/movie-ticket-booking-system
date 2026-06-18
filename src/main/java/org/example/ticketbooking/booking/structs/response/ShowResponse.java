package org.example.ticketbooking.booking.structs.response;

import org.example.ticketbooking.booking.entity.Show;
import org.example.ticketbooking.booking.structs.enums.ShowStatus;

import java.time.LocalDateTime;

public record ShowResponse(Long id, Long movieId, String movieTitle, Long screenId, String screenName,
                           String theaterName, String cityName, LocalDateTime startTime,
                           LocalDateTime endTime, ShowStatus status) {
    public static ShowResponse from(Show s) {
        return new ShowResponse(
                s.getId(),
                s.getMovie().getId(),
                s.getMovie().getTitle(),
                s.getScreen().getId(),
                s.getScreen().getName(),
                s.getScreen().getTheater().getName(),
                s.getScreen().getTheater().getCity().getName(),
                s.getStartTime(),
                s.getEndTime(),
                s.getStatus()
        );
    }
}
