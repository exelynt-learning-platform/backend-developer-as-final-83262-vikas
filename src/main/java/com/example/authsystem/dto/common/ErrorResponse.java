package com.example.authsystem.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized error response")
public class ErrorResponse {

    @Schema(description = "Error timestamp")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP error title", example = "Bad Request")
    private String error;

    @Schema(description = "Error message description", example = "End time must be after start time")
    private String message;

    @Schema(description = "Requested request path", example = "/api/reservations")
    private String path;

    @Schema(description = "Detailed field validation errors, if applicable")
    private Map<String, String> validationErrors;
}
