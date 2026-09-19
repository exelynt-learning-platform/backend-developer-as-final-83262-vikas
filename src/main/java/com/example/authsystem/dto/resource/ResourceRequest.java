package com.example.authsystem.dto.resource;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resource creation or update request")
public class ResourceRequest {

    @NotBlank(message = "Resource name is required")
    @Size(max = 100, message = "Resource name must not exceed 100 characters")
    @Schema(description = "Name of the resource", example = "Conference Room A")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Detailed description", example = "Main conference room with 4K projector and seating for 20")
    private String description;

    @NotBlank(message = "Resource type is required")
    @Size(max = 50, message = "Type must not exceed 50 characters")
    @Schema(description = "Type/Category of the resource", example = "CONFERENCE_ROOM")
    private String type;

    @NotNull(message = "Availability status is required")
    @Schema(description = "Whether the resource is active/available for booking", example = "true")
    private Boolean available;
}
