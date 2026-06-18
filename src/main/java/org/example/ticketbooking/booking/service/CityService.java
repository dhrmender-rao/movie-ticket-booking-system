package org.example.ticketbooking.booking.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketbooking.booking.structs.request.CreateCityRequest;
import org.example.ticketbooking.booking.structs.response.CityResponse;
import org.example.ticketbooking.booking.entity.City;
import org.example.ticketbooking.booking.repository.CityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;

    @Transactional
    public CityResponse createCity(CreateCityRequest request) {
        City city = City.builder()
                .name(request.name())
                .state(request.state())
                .country(request.country())
                .build();
        return CityResponse.from(cityRepository.save(city));
    }

    public List<CityResponse> getAllCities() {
        return cityRepository.findAll().stream().map(CityResponse::from).toList();
    }

}
