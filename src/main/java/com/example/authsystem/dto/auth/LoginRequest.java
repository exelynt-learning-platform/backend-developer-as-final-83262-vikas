package com.example.authsystem.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Login credentials request")
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    @Schema(description = "Username or Email address of the user", example = "admin")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Password of the user", example = "Admin@123")
    private String password;
}
