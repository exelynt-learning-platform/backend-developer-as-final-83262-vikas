package com.example.authsystem.controller;

import com.example.authsystem.dto.auth.LoginRequest;
import com.example.authsystem.dto.auth.LoginResponse;
import com.example.authsystem.dto.resource.ResourceRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate admin to obtain valid JWT
        MvcResult adminResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                LoginRequest.builder().username("admin").password("Admin@123").build())))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse adminResponse = objectMapper.readValue(
                adminResult.getResponse().getContentAsString(), LoginResponse.class);
        adminToken = "Bearer " + adminResponse.getToken();

        // Authenticate standard user to obtain valid JWT
        MvcResult userResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                LoginRequest.builder().username("user").password("User@123").build())))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse userResponse = objectMapper.readValue(
                userResult.getResponse().getContentAsString(), LoginResponse.class);
        userToken = "Bearer " + userResponse.getToken();
    }

    @Test
    @DisplayName("ADMIN should be able to create a new resource")
    void testAdmin_createResource_success() throws Exception {
        ResourceRequest request = ResourceRequest.builder()
                .name("Executive Boardroom")
                .description("Boardroom with smart board and video conference")
                .type("BOARDROOM")
                .available(true)
                .build();

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Executive Boardroom")))
                .andExpect(jsonPath("$.type", is("BOARDROOM")))
                .andExpect(jsonPath("$.available", is(true)));
    }

    @Test
    @DisplayName("ADMIN should be able to update an existing resource")
    void testAdmin_updateResource_success() throws Exception {
        // First create a resource to update
        ResourceRequest createReq = ResourceRequest.builder()
                .name("Room To Update")
                .description("Initial description")
                .type("ROOM")
                .available(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/resources")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        ResourceRequest updateReq = ResourceRequest.builder()
                .name("Updated Room Name")
                .description("Updated description")
                .type("CONFERENCE")
                .available(false)
                .build();

        mockMvc.perform(put("/api/resources/" + id)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Room Name")))
                .andExpect(jsonPath("$.available", is(false)));
    }

    @Test
    @DisplayName("ADMIN should be able to delete a resource")
    void testAdmin_deleteResource_success() throws Exception {
        ResourceRequest createReq = ResourceRequest.builder()
                .name("Room To Delete")
                .description("To be deleted")
                .type("TEMP")
                .available(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/resources")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/resources/" + id)
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        // Verify resource is gone
        mockMvc.perform(get("/api/resources/" + id)
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("USER should be able to view resources")
    void testUser_viewResources_success() throws Exception {
        mockMvc.perform(get("/api/resources")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("USER should NOT be able to create a resource (403 Forbidden)")
    void testUser_createResource_forbidden() throws Exception {
        ResourceRequest request = ResourceRequest.builder()
                .name("Unauthorized Resource")
                .description("Should be rejected")
                .type("EQUIPMENT")
                .available(true)
                .build();

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER should NOT be able to update a resource (403 Forbidden)")
    void testUser_updateResource_forbidden() throws Exception {
        ResourceRequest request = ResourceRequest.builder()
                .name("Unauthorized Update")
                .description("Should be rejected")
                .type("EQUIPMENT")
                .available(true)
                .build();

        mockMvc.perform(put("/api/resources/1")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER should NOT be able to delete a resource (403 Forbidden)")
    void testUser_deleteResource_forbidden() throws Exception {
        mockMvc.perform(delete("/api/resources/1")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request should return 401 Unauthorized")
    void testUnauthenticated_accessResources_unauthorized() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Validation failure on resource creation returns 400 Bad Request")
    void testCreateResource_validationErrors() throws Exception {
        ResourceRequest request = ResourceRequest.builder()
                .name("") // Blank name
                .type("")
                .available(null)
                .build();

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.available", notNullValue()));
    }
}
