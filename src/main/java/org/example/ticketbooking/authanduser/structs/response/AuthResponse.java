package org.example.ticketbooking.authanduser.structs.response;

public record AuthResponse(String token, String email, String role) {}
