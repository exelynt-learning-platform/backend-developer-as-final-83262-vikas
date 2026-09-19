package com.example.authsystem.controller;

import com.example.authsystem.dto.common.PagedResponse;
import com.example.authsystem.dto.resource.ResourceRequest;
import com.example.authsystem.dto.resource.ResourceResponse;
import com.example.authsystem.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.example.authsystem.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Endpoints for managing system resources")
@SecurityRequirement(name = "BearerAuth")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('" + Role.ROLE_ADMIN + "', '" + Role.ROLE_USER + "')")
    @Operation(summary = "List all resources", description = "Accessible by both ADMIN and USER roles. Supports pagination and sorting.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of resources successfully retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    })
    public ResponseEntity<PagedResponse<ResourceResponse>> getAllResources(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ResourceResponse> page = resourceService.getAllResources(pageable);
        return ResponseEntity.ok(PagedResponse.of(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('" + Role.ROLE_ADMIN + "', '" + Role.ROLE_USER + "')")
    @Operation(summary = "Get resource by ID", description = "Accessible by both ADMIN and USER roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resource found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public ResponseEntity<ResourceResponse> getResourceById(@PathVariable Long id) {
        ResourceResponse resource = resourceService.getResourceById(id);
        return ResponseEntity.ok(resource);
    }

    @PostMapping
    @PreAuthorize("hasRole('" + Role.ROLE_ADMIN + "')")
    @Operation(summary = "Create a new resource", description = "Accessible ONLY by ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Resource successfully created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "409", description = "Resource name already exists")
    })
    public ResponseEntity<ResourceResponse> createResource(@Valid @RequestBody ResourceRequest request) {
        ResourceResponse created = resourceService.createResource(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('" + Role.ROLE_ADMIN + "')")
    @Operation(summary = "Update an existing resource", description = "Accessible ONLY by ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resource successfully updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "409", description = "Resource name conflict")
    })
    public ResponseEntity<ResourceResponse> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody ResourceRequest request) {
        ResourceResponse updated = resourceService.updateResource(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('" + Role.ROLE_ADMIN + "')")
    @Operation(summary = "Delete a resource", description = "Accessible ONLY by ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Resource successfully deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}

