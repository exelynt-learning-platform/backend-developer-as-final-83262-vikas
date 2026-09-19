package com.example.authsystem.dto.resource;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resource details response")
public class ResourceResponse {

    @Schema(description = "Resource unique identifier", example = "1")
    private Long id;

    @Schema(description = "Resource name", example = "Conference Room A")
    private String name;

    @Schema(description = "Detailed description", example = "Main conference room with 4K projector")
    private String description;

    @Schema(description = "Resource type", example = "CONFERENCE_ROOM")
    private String type;

    @Schema(description = "Availability status", example = "true")
    private Boolean available;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
