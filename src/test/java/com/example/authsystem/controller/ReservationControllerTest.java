package com.example.authsystem.controller;

import com.example.authsystem.dto.auth.LoginRequest;
import com.example.authsystem.dto.auth.LoginResponse;
import com.example.authsystem.dto.auth.RegisterRequest;
import com.example.authsystem.dto.reservation.ReservationRequest;
import com.example.authsystem.dto.reservation.ReservationResponse;
import com.example.authsystem.dto.reservation.ReservationUpdateRequest;
import com.example.authsystem.entity.ReservationStatus;
import com.example.authsystem.entity.Resource;
import com.example.authsystem.entity.Role;
import com.example.authsystem.repository.ResourceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceRepository resourceRepository;

    private String adminToken;
    private String user1Token;
    private String user2Token;
    private Long testResourceId;

    @BeforeEach
    void setUp() throws Exception {
        // Obtain Admin Token
        MvcResult adminResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                LoginRequest.builder().username("admin").password("Admin@123").build())))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = "Bearer " + objectMapper.readValue(
                adminResult.getResponse().getContentAsString(), LoginResponse.class).getToken();

        // Obtain User 1 Token
        MvcResult user1Result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                LoginRequest.builder().username("user").password("User@123").build())))
                .andExpect(status().isOk())
                .andReturn();
        user1Token = "Bearer " + objectMapper.readValue(
                user1Result.getResponse().getContentAsString(), LoginResponse.class).getToken();

        // Register and obtain User 2 Token for multi-tenant isolation testing
        RegisterRequest user2Req = RegisterRequest.builder()
                .username("user2")
                .email("user2@example.com")
                .password("User2@123")
                .role(Role.USER)
                .build();
        MvcResult user2Result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2Req)))
                .andReturn();

        if (user2Result.getResponse().getStatus() == 201) {
            user2Token = "Bearer " + objectMapper.readValue(
                    user2Result.getResponse().getContentAsString(), LoginResponse.class).getToken();
        } else {
            MvcResult u2Login = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    LoginRequest.builder().username("user2").password("User2@123").build())))
                    .andExpect(status().isOk())
                    .andReturn();
            user2Token = "Bearer " + objectMapper.readValue(
                    u2Login.getResponse().getContentAsString(), LoginResponse.class).getToken();
        }

        // Get an available resource ID
        Resource resource = resourceRepository.findAll().stream()
                .filter(Resource::getAvailable)
                .findFirst()
                .orElseGet(() -> resourceRepository.save(Resource.builder()
                        .name("Test Resource " + System.currentTimeMillis())
                        .type("TEST")
                        .available(true)
                        .build()));
        testResourceId = resource.getId();
    }

    @Test
    @DisplayName("USER can create a reservation; identity is strictly taken from JWT")
    void testUser_createReservation_success() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime end = start.plusHours(2);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(testResourceId)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("150.00"))
                .status(ReservationStatus.PENDING)
                .build();

        MvcResult result = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.username", is("user")))
                .andExpect(jsonPath("$.price", is(150.00)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andReturn();

        ReservationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ReservationResponse.class);
        assertEquals("user", response.getUsername());
    }

    @Test
    @DisplayName("USER can view their own reservation")
    void testUser_viewOwnReservation_success() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withNano(0);
        LocalDateTime end = start.plusHours(2);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(testResourceId)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("200.00"))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long resId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/reservations/" + resId)
                        .header("Authorization", user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(resId.intValue())))
                .andExpect(jsonPath("$.username", is("user")));
    }

    @Test
    @DisplayName("USER CANNOT view another user's reservation (403 Forbidden)")
    void testUser_cannotViewAnotherUserReservation_forbidden() throws Exception {
        // User 1 creates a reservation
        LocalDateTime start = LocalDateTime.now().plusDays(3).withNano(0);
        LocalDateTime end = start.plusHours(2);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(testResourceId)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("250.00"))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long user1ResId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // User 2 attempts to view User 1's reservation
        mockMvc.perform(get("/api/reservations/" + user1ResId)
                        .header("Authorization", user2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("USER CANNOT update or delete another user's reservation (403 Forbidden)")
    void testUser_cannotUpdateOrDeleteAnotherUserReservation_forbidden() throws Exception {
        // User 1 creates a reservation
        LocalDateTime start = LocalDateTime.now().plusDays(4).withNano(0);
        LocalDateTime end = start.plusHours(2);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(testResourceId)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("300.00"))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long user1ResId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // User 2 attempts to update User 1's reservation
        ReservationUpdateRequest updateReq = ReservationUpdateRequest.builder()
                .startTime(start.plusHours(1))
                .endTime(end.plusHours(1))
                .price(new BigDecimal("350.00"))
                .status(ReservationStatus.CONFIRMED)
                .build();

        mockMvc.perform(put("/api/reservations/" + user1ResId)
                        .header("Authorization", user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // User 2 attempts to delete User 1's reservation
        mockMvc.perform(delete("/api/reservations/" + user1ResId)
                        .header("Authorization", user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN can view any user's reservation")
    void testAdmin_canViewAnyUserReservation_success() throws Exception {
        // User 1 creates a reservation
        LocalDateTime start = LocalDateTime.now().plusDays(5).withNano(0);
        LocalDateTime end = start.plusHours(1);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(testResourceId)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("120.00"))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long resId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Admin accesses User 1's reservation
        mockMvc.perform(get("/api/reservations/" + resId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(resId.intValue())))
                .andExpect(jsonPath("$.username", is("user")));
    }

    @Test
    @DisplayName("Validation failure: endTime before startTime returns 400 Bad Request")
    void testReservation_endTimeBeforeStartTime_badRequest() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.minusHours(1); // Invalid: ends before it starts

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(testResourceId)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("End time must be strictly after start time")));
    }

    @Test
    @DisplayName("Validation failure: negative price returns 400 Bad Request")
    void testReservation_negativePrice_badRequest() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(testResourceId)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("-50.00")) // Invalid negative price
                .build();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Filtering reservations by status and price range")
    void testReservation_filtering() throws Exception {
        // Create confirmed reservation with price 100
        ReservationRequest req1 = ReservationRequest.builder()
                .resourceId(testResourceId)
                .startTime(LocalDateTime.now().plusDays(6))
                .endTime(LocalDateTime.now().plusDays(6).plusHours(2))
                .price(new BigDecimal("100.00"))
                .status(ReservationStatus.CONFIRMED)
                .build();
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated());

        // Create cancelled reservation with price 500
        ReservationRequest req2 = ReservationRequest.builder()
                .resourceId(testResourceId)
                .startTime(LocalDateTime.now().plusDays(7))
                .endTime(LocalDateTime.now().plusDays(7).plusHours(2))
                .price(new BigDecimal("500.00"))
                .status(ReservationStatus.CANCELLED)
                .build();
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated());

        // Filter: status=CONFIRMED
        mockMvc.perform(get("/api/reservations?status=CONFIRMED")
                        .header("Authorization", user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.[*].status", everyItem(is("CONFIRMED"))));

        // Filter: minPrice=200&maxPrice=600
        mockMvc.perform(get("/api/reservations?minPrice=200&maxPrice=600")
                        .header("Authorization", user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.[*].price", everyItem(greaterThanOrEqualTo(200.0))));
    }

    @Test
    @DisplayName("Pagination returns proper pagination metadata")
    void testReservation_pagination() throws Exception {
        mockMvc.perform(get("/api/reservations?page=0&size=2")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber", is(0)))
                .andExpect(jsonPath("$.pageSize", is(2)))
                .andExpect(jsonPath("$.totalElements", notNullValue()))
                .andExpect(jsonPath("$.totalPages", notNullValue()))
                .andExpect(jsonPath("$.first", is(true)));
    }

    @Test
    @DisplayName("Sorting by price descending returns ordered list")
    void testReservation_sorting_priceDesc() throws Exception {
        mockMvc.perform(get("/api/reservations?sort=price,desc")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("Rejection of unsafe/invalid sort field returns 400 Bad Request")
    void testReservation_unsafeSortField_badRequest() throws Exception {
        mockMvc.perform(get("/api/reservations?sort=maliciousProperty,desc")
                        .header("Authorization", adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Invalid sort field")));
    }

    @Test
    @DisplayName("USER cannot self-assign CONFIRMED status at creation; strictly forced to PENDING")
    void testUser_cannotForceConfirmedStatus() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(10).withNano(0);
        LocalDateTime end = start.plusHours(2);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(testResourceId)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("180.00"))
                .status(ReservationStatus.CONFIRMED) // USER attempts to bypass workflow
                .build();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING"))); // Must be forced to PENDING
    }

    @Test
    @DisplayName("Overlapping reservation on same resource returns 409 Conflict")
    void testReservation_overlapConflict() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(15).withNano(0);
        LocalDateTime end = start.plusHours(3);

        ReservationRequest initial = ReservationRequest.builder()
                .resourceId(testResourceId)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("100.00"))
                .build();

        // First reservation succeeds
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated());

        // Second reservation overlapping with first time window [start + 1h, end + 1h]
        ReservationRequest overlapping = ReservationRequest.builder()
                .resourceId(testResourceId)
                .startTime(start.plusHours(1))
                .endTime(end.plusHours(1))
                .price(new BigDecimal("120.00"))
                .build();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlapping)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already reserved")));
    }
}

