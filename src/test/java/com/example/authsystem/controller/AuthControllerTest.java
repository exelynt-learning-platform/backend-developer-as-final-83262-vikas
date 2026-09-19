package com.example.authsystem.controller;

import com.example.authsystem.dto.auth.LoginRequest;
import com.example.authsystem.dto.auth.RegisterRequest;
import com.example.authsystem.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should successfully authenticate with valid username and password")
    void testLogin_successWithUsername() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("admin")
                .password("Admin@123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }

    @Test
    @DisplayName("Should successfully authenticate with valid email and password")
    void testLogin_successWithEmail() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("user@example.com")
                .password("User@123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("user")))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for invalid password")
    void testLogin_invalidPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("admin")
                .password("WrongPassword")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for non-existent user")
    void testLogin_nonExistentUser() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("nonexistent")
                .password("Password@123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when required login fields are missing")
    void testLogin_missingFields() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("")
                .password("")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.username", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.password", notNullValue()));
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void testRegister_success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("newtester")
                .email("tester@example.com")
                .password("Tester@123")
                .role(Role.USER)
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("newtester")))
                .andExpect(jsonPath("$.email", is("tester@example.com")))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    @DisplayName("Should return 409 Conflict when registering an existing username")
    void testRegister_duplicateUsername() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("admin")
                .email("unique_admin@example.com")
                .password("Admin@123")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")));
    }
}
