package org.example.ticketbooking.booking.structs.response;

import java.time.LocalDateTime;
import java.util.List;

public record HoldResponse(List<ShowSeatResponse> seats, LocalDateTime expiresAt) {}
