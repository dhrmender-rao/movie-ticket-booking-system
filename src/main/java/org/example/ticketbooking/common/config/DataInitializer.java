package org.example.ticketbooking.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketbooking.booking.entity.RefundPolicy;
import org.example.ticketbooking.authanduser.entity.User;
import org.example.ticketbooking.authanduser.structs.enums.Role;
import org.example.ticketbooking.booking.repository.RefundPolicyRepository;
import org.example.ticketbooking.authanduser.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefundPolicyRepository refundPolicyRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            seedAdmin();
            seedRefundPolicies();
        };
    }

    private void seedAdmin() {
        if (!userRepository.existsByEmail("admin@movieticket.com")) {
            User admin = User.builder()
                    .name("System Admin")
                    .email("admin@movieticket.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Seeded admin user: admin@movieticket.com / admin123");
        }
    }

    private void seedRefundPolicies() {
        if (refundPolicyRepository.count() == 0) {

            refundPolicyRepository.save(RefundPolicy.builder()
                    .hoursBeforeShow(24)
                    .refundPercentage(new BigDecimal("50.00"))
                    .description("50% refund if cancelled 24-48 hours before show")
                    .build());

            refundPolicyRepository.save(RefundPolicy.builder()
                    .hoursBeforeShow(2)
                    .refundPercentage(new BigDecimal("25.00"))
                    .description("25% refund if cancelled 2-24 hours before show")
                    .build());

            log.info("Seeded default refund policies");
        }
    }
}
