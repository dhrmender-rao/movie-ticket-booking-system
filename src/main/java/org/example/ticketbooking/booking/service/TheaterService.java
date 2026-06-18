package org.example.ticketbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.request.CreateTheaterRequest;
import org.example.ticketbooking.booking.structs.response.TheaterResponse;
import org.example.ticketbooking.booking.entity.City;
import org.example.ticketbooking.booking.entity.Theater;
import org.example.ticketbooking.common.exception.AppException;
import org.example.ticketbooking.booking.repository.CityRepository;
import org.example.ticketbooking.booking.repository.TheaterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TheaterService {

    private final TheaterRepository theaterRepository;
    private final CityRepository cityRepository;

    @Transactional
    public TheaterResponse createTheater(CreateTheaterRequest request) {
        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> AppException.notFound("City", request.cityId()));

        Theater theater = Theater.builder()
                .name(request.name())
                .address(request.address())
                .city(city)
                .build();

        return TheaterResponse.from(theaterRepository.save(theater));
    }

    public List<TheaterResponse> getTheatersByCity(Long cityId) {
        return theaterRepository.findByCityId(cityId).stream()
                .map(TheaterResponse::from).toList();
    }

}
