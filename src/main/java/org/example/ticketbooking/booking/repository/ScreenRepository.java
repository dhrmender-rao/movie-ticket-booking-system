package org.example.ticketbooking.booking.repository;

import org.example.ticketbooking.booking.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Long> {
    List<Screen> findByTheaterId(Long theaterId);
}
