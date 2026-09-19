package com.example.authsystem.dto.reservation;

import com.example.authsystem.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Reservation details response")
public class ReservationResponse {

    @Schema(description = "Reservation unique identifier", example = "1")
    private Long id;

    @Schema(description = "Associated resource ID", example = "5")
    private Long resourceId;

    @Schema(description = "Associated resource name", example = "Conference Room A")
    private String resourceName;

    @Schema(description = "Associated resource type", example = "CONFERENCE_ROOM")
    private String resourceType;

    @Schema(description = "Booking user ID", example = "2")
    private Long userId;

    @Schema(description = "Booking username", example = "user")
    private String username;

    @Schema(description = "Booking user email", example = "user@example.com")
    private String userEmail;

    @Schema(description = "Reservation start time")
    private LocalDateTime startTime;

    @Schema(description = "Reservation end time")
    private LocalDateTime endTime;

    @Schema(description = "Reservation price", example = "150.00")
    private BigDecimal price;

    @Schema(description = "Reservation status", example = "CONFIRMED")
    private ReservationStatus status;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
