package com.example.authsystem.dto.reservation;

import com.example.authsystem.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Reservation creation request payload. Note: The booking user is automatically resolved from the authenticated JWT token.")
public class ReservationRequest {

    @NotNull(message = "Resource ID is required")
    @Schema(description = "ID of the resource to reserve", example = "1")
    private Long resourceId;

    @NotNull(message = "Start time is required")
    @Schema(description = "Reservation start time (ISO 8601)", example = "2026-10-01T10:00:00")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Schema(description = "Reservation end time (ISO 8601)", example = "2026-10-01T12:00:00")
    private LocalDateTime endTime;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    @Schema(description = "Reservation price", example = "150.00")
    private BigDecimal price;

    @Schema(description = "Initial status (defaults to PENDING if not provided)", example = "PENDING")
    private ReservationStatus status;
}
