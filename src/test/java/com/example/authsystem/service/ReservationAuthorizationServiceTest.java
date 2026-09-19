package com.example.authsystem.service;

import com.example.authsystem.entity.Reservation;
import com.example.authsystem.entity.Role;
import com.example.authsystem.entity.User;
import com.example.authsystem.exception.AccessDeniedCustomException;
import com.example.authsystem.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationAuthorizationServiceTest {

    private ReservationAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new ReservationAuthorizationService();
    }

    @Test
    @DisplayName("Admin user should have unrestricted access")
    void testAdminAccess() {
        User owner = User.builder().id(10L).username("owner").role(Role.USER).build();
        Reservation reservation = Reservation.builder().id(100L).user(owner).build();

        User adminUser = User.builder().id(999L).username("admin").role(Role.ADMIN).build();
        CustomUserDetails adminDetails = new CustomUserDetails(adminUser);

        assertDoesNotThrow(() -> authorizationService.checkOwnershipOrAdmin(reservation, adminDetails));
    }

    @Test
    @DisplayName("Owner user should have access to own reservation")
    void testOwnerAccess() {
        User owner = User.builder().id(10L).username("owner").role(Role.USER).build();
        Reservation reservation = Reservation.builder().id(100L).user(owner).build();

        CustomUserDetails ownerDetails = new CustomUserDetails(owner);

        assertDoesNotThrow(() -> authorizationService.checkOwnershipOrAdmin(reservation, ownerDetails));
    }

    @Test
    @DisplayName("Non-owner user should be denied access")
    void testNonOwnerAccessDenied() {
        User owner = User.builder().id(10L).username("owner").role(Role.USER).build();
        Reservation reservation = Reservation.builder().id(100L).user(owner).build();

        User anotherUser = User.builder().id(20L).username("another").role(Role.USER).build();
        CustomUserDetails anotherUserDetails = new CustomUserDetails(anotherUser);

        assertThrows(AccessDeniedCustomException.class, () ->
                authorizationService.checkOwnershipOrAdmin(reservation, anotherUserDetails));
    }
}
