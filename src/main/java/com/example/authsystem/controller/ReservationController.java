package com.example.authsystem.controller;

import com.example.authsystem.dto.common.PagedResponse;
import com.example.authsystem.dto.reservation.ReservationRequest;
import com.example.authsystem.dto.reservation.ReservationResponse;
import com.example.authsystem.dto.reservation.ReservationUpdateRequest;
import com.example.authsystem.entity.ReservationStatus;
import com.example.authsystem.security.CustomUserDetails;
import com.example.authsystem.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Endpoints for creating, managing, and viewing reservations")
@SecurityRequirement(name = "BearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Create a reservation", description = "Creates a new reservation. User identity is strictly resolved from the authenticated JWT token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reservation successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid reservation details (e.g. endTime before startTime, negative price, unavailable resource)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        ReservationResponse created = reservationService.createReservation(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List reservations with filtering, pagination, and sorting",
            description = "ADMIN users see all reservations; USER role only sees their own reservations. Supports filtering by status, minPrice, and maxPrice.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservations retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter, pagination, or sort parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PagedResponse<ReservationResponse>> getReservations(
            @Parameter(description = "Filter by status (PENDING, CONFIRMED, CANCELLED)")
            @RequestParam(required = false) ReservationStatus status,

            @Parameter(description = "Filter by minimum price")
            @RequestParam(required = false) BigDecimal minPrice,

            @Parameter(description = "Filter by maximum price")
            @RequestParam(required = false) BigDecimal maxPrice,

            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort fields (e.g., 'price,desc' or 'startTime,asc'). Safe fields: id, startTime, endTime, price, status, createdAt, updatedAt")
            @RequestParam(required = false) String[] sort,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        PagedResponse<ReservationResponse> response = reservationService.getReservations(
                status, minPrice, maxPrice, page, size, sort, currentUser
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by ID", description = "ADMIN users can access any reservation. Regular users can only access their own reservation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot view another user's reservation"),
            @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<ReservationResponse> getReservationById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        ReservationResponse reservation = reservationService.getReservationById(id, currentUser);
        return ResponseEntity.ok(reservation);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing reservation", description = "ADMIN users can update any reservation. Regular users can only update their own reservation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid update parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot modify another user's reservation"),
            @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody ReservationUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        ReservationResponse updated = reservationService.updateReservation(id, request, currentUser);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete or cancel a reservation", description = "ADMIN users can delete any reservation. Regular users can only delete/cancel their own reservation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Reservation successfully deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot delete another user's reservation"),
            @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<Void> deleteReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        reservationService.deleteReservation(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
