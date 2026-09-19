package com.example.authsystem.service;

import com.example.authsystem.dto.resource.ResourceRequest;
import com.example.authsystem.dto.resource.ResourceResponse;
import com.example.authsystem.entity.Resource;
import com.example.authsystem.exception.ResourceConflictException;
import com.example.authsystem.exception.ResourceNotFoundException;
import com.example.authsystem.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional(readOnly = true)
    public Page<ResourceResponse> getAllResources(Pageable pageable) {
        return resourceRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        Resource resource = findEntityById(id);
        return mapToResponse(resource);
    }

    @Transactional
    public ResourceResponse createResource(ResourceRequest request) {
        if (resourceRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new ResourceConflictException("Resource with name '" + request.getName() + "' already exists");
        }

        Resource resource = Resource.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .type(request.getType().trim().toUpperCase())
                .available(request.getAvailable())
                .build();

        Resource saved = resourceRepository.save(resource);
        return mapToResponse(saved);
    }

    @Transactional
    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        Resource resource = findEntityById(id);

        if (!resource.getName().equalsIgnoreCase(request.getName().trim()) &&
                resourceRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new ResourceConflictException("Resource with name '" + request.getName() + "' already exists");
        }

        resource.setName(request.getName().trim());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType().trim().toUpperCase());
        resource.setAvailable(request.getAvailable());

        Resource updated = resourceRepository.save(resource);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteResource(Long id) {
        Resource resource = findEntityById(id);
        resourceRepository.delete(resource);
    }

    public Resource findEntityById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));
    }

    public ResourceResponse mapToResponse(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .description(resource.getDescription())
                .type(resource.getType())
                .available(resource.getAvailable())
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }
}
