package com.example.authsystem.dto.auth;

import com.example.authsystem.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "JWT authentication response payload")
public class LoginResponse {

    @Schema(description = "JWT Bearer access token")
    private String token;

    @Schema(description = "Token type", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "Username", example = "admin")
    private String username;

    @Schema(description = "User email", example = "admin@example.com")
    private String email;

    @Schema(description = "Assigned user role", example = "ADMIN")
    private Role role;
}
