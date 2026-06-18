package org.example.ticketbooking.booking.structs.response;

import org.example.ticketbooking.booking.entity.City;

public record CityResponse(Long id, String name, String state, String country) {
    public static CityResponse from(City c) {
        return new CityResponse(c.getId(), c.getName(), c.getState(), c.getCountry());
    }
}
