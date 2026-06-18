package org.example.ticketbooking.common.service;

import org.example.ticketbooking.authanduser.service.AuthService;
import org.example.ticketbooking.authanduser.structs.request.LoginRequest;
import org.example.ticketbooking.authanduser.structs.request.RegisterRequest;
import org.example.ticketbooking.authanduser.structs.response.AuthResponse;
import org.example.ticketbooking.authanduser.entity.User;
import org.example.ticketbooking.authanduser.structs.enums.Role;
import org.example.ticketbooking.common.exception.AppException;
import org.example.ticketbooking.authanduser.repository.UserRepository;
import org.example.ticketbooking.common.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks
    AuthService authService;

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest("Alice", "alice@test.com", "password123");

        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u = User.builder().id(1L).name(u.getName()).email(u.getEmail())
                    .passwordHash(u.getPasswordHash()).role(u.getRole()).build();
            return u;
        });
        when(jwtUtil.generateToken("alice@test.com", "CUSTOMER")).thenReturn("mock-token");

        AuthResponse response = authService.register(req);

        assertThat(response.token()).isEqualTo("mock-token");
        assertThat(response.email()).isEqualTo("alice@test.com");
        assertThat(response.role()).isEqualTo("CUSTOMER");
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);
        RegisterRequest req = new RegisterRequest("Bob", "existing@test.com", "pass123");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void login_success() {
        User user = User.builder().id(1L).email("alice@test.com")
                .passwordHash("hashed").role(Role.CUSTOMER).build();

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("alice@test.com", "CUSTOMER")).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("alice@test.com", "password"));

        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void login_wrongPassword_throws() {
        User user = User.builder().id(1L).email("alice@test.com")
                .passwordHash("hashed").role(Role.CUSTOMER).build();

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@test.com", "wrong")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_userNotFound_throws() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@test.com", "pass")))
                .isInstanceOf(AppException.class);
    }
}
