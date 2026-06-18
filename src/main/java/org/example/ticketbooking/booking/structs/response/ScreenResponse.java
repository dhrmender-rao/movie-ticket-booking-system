package org.example.ticketbooking.booking.structs.response;

import org.example.ticketbooking.booking.entity.Screen;

public record ScreenResponse(Long id, String name, int totalRows, int totalCols,
                              Long theaterId, String theaterName) {
    public static ScreenResponse from(Screen s) {
        return new ScreenResponse(s.getId(), s.getName(), s.getTotalRows(), s.getTotalCols(),
                s.getTheater().getId(), s.getTheater().getName());
    }
}
