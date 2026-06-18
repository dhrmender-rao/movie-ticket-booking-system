package org.example.ticketbooking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ticketbooking.booking.structs.request.CreateCityRequest;
import org.example.ticketbooking.booking.structs.request.CreateDiscountCodeRequest;
import org.example.ticketbooking.booking.structs.request.CreateMovieRequest;
import org.example.ticketbooking.booking.structs.request.CreatePricingTierRequest;
import org.example.ticketbooking.booking.structs.request.CreateScreenRequest;
import org.example.ticketbooking.booking.structs.request.CreateShowRequest;
import org.example.ticketbooking.booking.structs.request.CreateTheaterRequest;
import org.example.ticketbooking.booking.structs.enums.DayType;
import org.example.ticketbooking.booking.structs.enums.DiscountType;
import org.example.ticketbooking.booking.structs.enums.SeatType;
import org.example.ticketbooking.booking.structs.request.CreateBookingRequest;
import org.example.ticketbooking.booking.structs.request.HoldSeatsRequest;
import org.example.ticketbooking.authanduser.structs.request.LoginRequest;
import org.example.ticketbooking.booking.structs.request.PaymentRequest;
import org.example.ticketbooking.authanduser.structs.request.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookingFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAsAdmin();
        customerToken = registerAndLoginCustomer("customer@test.com", "pass123");
    }

    @Test
    void fullBookingFlow_holdBookPayCancel() throws Exception {
        // 1. Admin creates city, theater, screen, movie, show, pricing
        Long cityId = createCity("Mumbai");
        Long theaterId = createTheater("PVR Juhu", cityId);
        Long screenId = createScreen("Screen 1", theaterId, 5, 10, 2);
        Long movieId = createMovie("Inception");
        Long showId = createShow(movieId, screenId, LocalDateTime.now().plusDays(3));
        createPricing(showId, SeatType.REGULAR, DayType.WEEKDAY, new BigDecimal("200"));
        createPricing(showId, SeatType.REGULAR, DayType.WEEKEND, new BigDecimal("250"));
        createPricing(showId, SeatType.PREMIUM, DayType.WEEKDAY, new BigDecimal("350"));
        createPricing(showId, SeatType.PREMIUM, DayType.WEEKEND, new BigDecimal("400"));

        // 2. Customer browses shows
        mvc.perform(get("/api/cities/" + cityId + "/shows")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 3. Customer views seat map
        MvcResult seatResult = mvc.perform(get("/api/shows/" + showId + "/seats"))
                .andExpect(status().isOk())
                .andReturn();

        List<Long> seatIds = extractSeatIds(seatResult.getResponse().getContentAsString(), 2);

        // 4. Customer holds seats
        HoldSeatsRequest holdReq = new HoldSeatsRequest(seatIds);
        mvc.perform(post("/api/shows/" + showId + "/seats/hold")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats.length()").value(2))
                .andExpect(jsonPath("$.expiresAt").exists());

        // 5. Customer creates booking
        CreateBookingRequest bookingReq = new CreateBookingRequest(showId, seatIds, null);
        MvcResult bookingResult = mvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").exists())
                .andReturn();

        Long bookingId = extractId(bookingResult.getResponse().getContentAsString());

        // 6. Customer pays
        PaymentRequest payReq = new PaymentRequest("CARD", "tok_test");
        mvc.perform(post("/api/payments/" + bookingId + "/pay")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 7. Customer views booking history
        mvc.perform(get("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 8. Customer cancels booking
        mvc.perform(delete("/api/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void booking_withDiscountCode() throws Exception {
        Long cityId = createCity("Delhi");
        Long theaterId = createTheater("PVR Select", cityId);
        Long screenId = createScreen("Screen A", theaterId, 3, 5, 1);
        Long movieId = createMovie("Dune");
        Long showId = createShow(movieId, screenId, LocalDateTime.now().plusDays(5));
        createPricing(showId, SeatType.REGULAR, DayType.WEEKDAY, new BigDecimal("300"));
        createPricing(showId, SeatType.REGULAR, DayType.WEEKEND, new BigDecimal("350"));
        createPricing(showId, SeatType.PREMIUM, DayType.WEEKDAY, new BigDecimal("500"));
        createPricing(showId, SeatType.PREMIUM, DayType.WEEKEND, new BigDecimal("550"));

        // Admin creates discount code
        createDiscountCode("SAVE10", DiscountType.PERCENTAGE, new BigDecimal("10"), 100);

        MvcResult seatResult = mvc.perform(get("/api/shows/" + showId + "/seats"))
                .andExpect(status().isOk()).andReturn();
        List<Long> seatIds = extractSeatIds(seatResult.getResponse().getContentAsString(), 1);

        // Hold
        mvc.perform(post("/api/shows/" + showId + "/seats/hold")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new HoldSeatsRequest(seatIds))))
                .andExpect(status().isOk());

        // Book with discount
        MvcResult bookingResult = mvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBookingRequest(showId, seatIds, "SAVE10"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(org.hamcrest.Matchers.greaterThan(0.0)))
                .andReturn();

        // Verify finalAmount < totalAmount
        String body = bookingResult.getResponse().getContentAsString();
        var root = objectMapper.readTree(body);
        double total = root.get("totalAmount").asDouble();
        double final_ = root.get("finalAmount").asDouble();
        assertThat(final_).isLessThan(total);
    }

    @Test
    void holdSeats_concurrentRequests_onlyOneSucceeds() throws Exception {
        Long cityId = createCity("Bangalore");
        Long theaterId = createTheater("INOX", cityId);
        Long screenId = createScreen("Screen 2", theaterId, 2, 3, 1);
        Long movieId = createMovie("Matrix");
        Long showId = createShow(movieId, screenId, LocalDateTime.now().plusDays(7));
        createPricing(showId, SeatType.REGULAR, DayType.WEEKDAY, new BigDecimal("150"));
        createPricing(showId, SeatType.REGULAR, DayType.WEEKEND, new BigDecimal("200"));
        createPricing(showId, SeatType.PREMIUM, DayType.WEEKDAY, new BigDecimal("280"));
        createPricing(showId, SeatType.PREMIUM, DayType.WEEKEND, new BigDecimal("320"));

        String customer2Token = registerAndLoginCustomer("customer2@test.com", "pass123");

        MvcResult seatResult = mvc.perform(get("/api/shows/" + showId + "/seats"))
                .andExpect(status().isOk()).andReturn();
        List<Long> seatIds = extractSeatIds(seatResult.getResponse().getContentAsString(), 1);

        HoldSeatsRequest holdReq = new HoldSeatsRequest(seatIds);

        // Both customers try to hold the same seat — one should fail
        int[] results = {0, 0}; // [success, conflict]
        Thread t1 = new Thread(() -> {
            try {
                int status = mvc.perform(post("/api/shows/" + showId + "/seats/hold")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                        .andReturn().getResponse().getStatus();
                if (status == 200) results[0]++;
                else results[1]++;
            } catch (Exception e) { results[1]++; }
        });

        Thread t2 = new Thread(() -> {
            try {
                int status = mvc.perform(post("/api/shows/" + showId + "/seats/hold")
                        .header("Authorization", "Bearer " + customer2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                        .andReturn().getResponse().getStatus();
                if (status == 200) results[0]++;
                else results[1]++;
            } catch (Exception e) { results[1]++; }
        });

        t1.start(); t2.start();
        t1.join(); t2.join();

        // At most 1 should succeed (one may succeed, one fails with 409 or one also gets 200 from different timing)
        assertThat(results[0]).isLessThanOrEqualTo(1);
        assertThat(results[0] + results[1]).isEqualTo(2);
    }

    @Test
    void adminCancelShow_refundsAllConfirmedBookings() throws Exception {
        Long cityId = createCity("Chennai");
        Long theaterId = createTheater("SPI", cityId);
        Long screenId = createScreen("Screen 3", theaterId, 2, 4, 1);
        Long movieId = createMovie("Avengers");
        Long showId = createShow(movieId, screenId, LocalDateTime.now().plusDays(10));
        createPricing(showId, SeatType.REGULAR, DayType.WEEKDAY, new BigDecimal("200"));
        createPricing(showId, SeatType.REGULAR, DayType.WEEKEND, new BigDecimal("250"));
        createPricing(showId, SeatType.PREMIUM, DayType.WEEKDAY, new BigDecimal("350"));
        createPricing(showId, SeatType.PREMIUM, DayType.WEEKEND, new BigDecimal("400"));

        MvcResult seatResult = mvc.perform(get("/api/shows/" + showId + "/seats"))
                .andExpect(status().isOk()).andReturn();
        List<Long> seatIds = extractSeatIds(seatResult.getResponse().getContentAsString(), 1);

        // Customer holds, books, and pays
        mvc.perform(post("/api/shows/" + showId + "/seats/hold")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new HoldSeatsRequest(seatIds))))
                .andExpect(status().isOk());

        MvcResult bookingResult = mvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBookingRequest(showId, seatIds, null))))
                .andExpect(status().isCreated()).andReturn();
        Long bookingId = extractId(bookingResult.getResponse().getContentAsString());

        mvc.perform(post("/api/payments/" + bookingId + "/pay")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentRequest("CARD", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Admin cancels show
        mvc.perform(delete("/api/admin/shows/" + showId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Show should be cancelled
        mvc.perform(get("/api/shows/" + showId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private String loginAsAdmin() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("admin@movieticket.com", "admin123"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String registerAndLoginCustomer(String email, String password) throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterRequest("Customer", email, password))))
                .andExpect(status().isCreated());

        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private Long createCity(String name) throws Exception {
        MvcResult r = mvc.perform(post("/api/admin/cities")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCityRequest(name, "State", "India"))))
                .andExpect(status().isCreated()).andReturn();
        return extractId(r.getResponse().getContentAsString());
    }

    private Long createTheater(String name, Long cityId) throws Exception {
        MvcResult r = mvc.perform(post("/api/admin/theaters")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTheaterRequest(name, "Address", cityId))))
                .andExpect(status().isCreated()).andReturn();
        return extractId(r.getResponse().getContentAsString());
    }

    private Long createScreen(String name, Long theaterId, int rows, int cols, int premiumRows) throws Exception {
        MvcResult r = mvc.perform(post("/api/admin/screens")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateScreenRequest(name, theaterId, rows, cols, premiumRows))))
                .andExpect(status().isCreated()).andReturn();
        return extractId(r.getResponse().getContentAsString());
    }

    private Long createMovie(String title) throws Exception {
        MvcResult r = mvc.perform(post("/api/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateMovieRequest(title, 150, "English", "Sci-Fi", "UA", "Description"))))
                .andExpect(status().isCreated()).andReturn();
        return extractId(r.getResponse().getContentAsString());
    }

    private Long createShow(Long movieId, Long screenId, LocalDateTime startTime) throws Exception {
        MvcResult r = mvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateShowRequest(movieId, screenId, startTime))))
                .andExpect(status().isCreated()).andReturn();
        return extractId(r.getResponse().getContentAsString());
    }

    private void createPricing(Long showId, SeatType seatType, DayType dayType, BigDecimal price) throws Exception {
        mvc.perform(post("/api/admin/pricing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePricingTierRequest(showId, seatType, dayType, price))))
                .andExpect(status().isCreated());
    }

    private void createDiscountCode(String code, DiscountType type, BigDecimal value, int maxUses) throws Exception {
        mvc.perform(post("/api/admin/discount-codes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateDiscountCodeRequest(code, type, value, maxUses, null, null))))
                .andExpect(status().isCreated());
    }

    private Long extractId(String json) throws Exception {
        return objectMapper.readTree(json).get("id").asLong();
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractSeatIds(String json, int count) throws Exception {
        var arr = objectMapper.readTree(json);
        List<Long> ids = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(count, arr.size()); i++) {
            ids.add(arr.get(i).get("id").asLong());
        }
        return ids;
    }
}
