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
@Schema(description = "Reservation update request payload")
public class ReservationUpdateRequest {

    @NotNull(message = "Start time is required")
    @Schema(description = "Updated start time (ISO 8601)", example = "2026-10-01T11:00:00")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Schema(description = "Updated end time (ISO 8601)", example = "2026-10-01T13:00:00")
    private LocalDateTime endTime;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    @Schema(description = "Updated reservation price", example = "175.00")
    private BigDecimal price;

    @NotNull(message = "Status is required")
    @Schema(description = "Updated reservation status", example = "CONFIRMED")
    private ReservationStatus status;
}
