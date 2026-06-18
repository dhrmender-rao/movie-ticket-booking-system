package org.example.ticketbooking.booking.structs.response;

import org.example.ticketbooking.booking.entity.Theater;

public record TheaterResponse(Long id, String name, String address, Long cityId, String cityName) {
    public static TheaterResponse from(Theater t) {
        return new TheaterResponse(t.getId(), t.getName(), t.getAddress(),
                t.getCity().getId(), t.getCity().getName());
    }
}
