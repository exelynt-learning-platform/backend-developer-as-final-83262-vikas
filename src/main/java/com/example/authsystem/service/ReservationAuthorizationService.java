package com.example.authsystem.service;

import com.example.authsystem.entity.Reservation;
import com.example.authsystem.entity.Role;
import com.example.authsystem.exception.AccessDeniedCustomException;
import com.example.authsystem.security.CustomUserDetails;
import org.springframework.stereotype.Service;

/**
 * Dedicated authorization service for reservation access checks,
 * eliminating cyclomatic complexity in business service methods and
 * using principal identifiers directly without redundant database lookups.
 */
@Service
public class ReservationAuthorizationService {

    public void checkOwnershipOrAdmin(Reservation reservation, CustomUserDetails currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return; // Admins have full access
        }

        // Direct comparison against principal ID from SecurityContext (zero extra DB lookups)
        Long reservationOwnerId = reservation.getUser().getId();
        if (!reservationOwnerId.equals(currentUser.getId())) {
            throw new AccessDeniedCustomException("You do not have permission to access another user's reservation");
        }
    }
}
