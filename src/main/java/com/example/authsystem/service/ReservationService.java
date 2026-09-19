package com.example.authsystem.service;

import com.example.authsystem.dto.common.PagedResponse;
import com.example.authsystem.dto.reservation.ReservationRequest;
import com.example.authsystem.dto.reservation.ReservationResponse;
import com.example.authsystem.dto.reservation.ReservationUpdateRequest;
import com.example.authsystem.entity.*;
import com.example.authsystem.exception.BadRequestException;
import com.example.authsystem.exception.ResourceConflictException;
import com.example.authsystem.exception.ResourceNotFoundException;
import com.example.authsystem.repository.ReservationRepository;
import com.example.authsystem.repository.ReservationSpecification;
import com.example.authsystem.repository.ResourceRepository;
import com.example.authsystem.repository.UserRepository;
import com.example.authsystem.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "startTime", "endTime", "price", "status", "createdAt", "updatedAt"
    );

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ReservationAuthorizationService authorizationService;

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request, CustomUserDetails currentUser) {
        validateReservationTimes(request.getStartTime(), request.getEndTime());
        validatePrice(request.getPrice());

        Resource resource = findResourceById(request.getResourceId());
        validateResourceAvailability(resource);
        validateNoOverlappingReservation(resource.getId(), null, request.getStartTime(), request.getEndTime());

        // Uses reference proxy to avoid redundant SELECT query on user entity
        User userProxy = userRepository.getReferenceById(currentUser.getId());

        // USER role is strictly forced to PENDING status at creation; only ADMIN may override
        ReservationStatus initialStatus = (currentUser.getRole() == Role.ADMIN && request.getStatus() != null)
                ? request.getStatus()
                : ReservationStatus.PENDING;

        Reservation reservation = Reservation.builder()
                .user(userProxy)
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(initialStatus)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReservationResponse> getReservations(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String[] sortParams,
            CustomUserDetails currentUser) {

        validatePaginationParameters(page, size);

        Sort sort = buildValidatedSort(sortParams);
        Pageable pageable = PageRequest.of(page, size, sort);

        // RBAC: If USER, restrict query directly via SQL join to their own user ID.
        // If ADMIN, query across all reservations.
        Long targetUserId = (currentUser.getRole() == Role.ADMIN) ? null : currentUser.getId();

        Specification<Reservation> spec = ReservationSpecification.filterReservations(
                targetUserId, status, minPrice, maxPrice
        );

        Page<ReservationResponse> reservationPage = reservationRepository.findAll(spec, pageable)
                .map(this::mapToResponse);

        return PagedResponse.of(reservationPage);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> findAuthorizedReservationList(Long userId, Pageable pageable) {
        return reservationRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id, CustomUserDetails currentUser) {
        Reservation reservation = findReservationById(id);
        authorizationService.checkOwnershipOrAdmin(reservation, currentUser);
        return mapToResponse(reservation);
    }

    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationUpdateRequest request, CustomUserDetails currentUser) {
        Reservation reservation = findReservationById(id);
        authorizationService.checkOwnershipOrAdmin(reservation, currentUser);

        validateReservationTimes(request.getStartTime(), request.getEndTime());
        validatePrice(request.getPrice());
        validateReservationStatus(request.getStatus());

        if (request.getStatus() != ReservationStatus.CANCELLED) {
            validateResourceAvailability(reservation.getResource());
            validateNoOverlappingReservation(
                    reservation.getResource().getId(), reservation.getId(), request.getStartTime(), request.getEndTime()
            );
        }

        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());
        reservation.setStatus(request.getStatus());

        Reservation updated = reservationRepository.save(reservation);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteReservation(Long id, CustomUserDetails currentUser) {
        Reservation reservation = findReservationById(id);
        authorizationService.checkOwnershipOrAdmin(reservation, currentUser);
        reservationRepository.delete(reservation);
    }

    private Reservation findReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + id));
    }

    private Resource findResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));
    }

    private void validateReservationTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BadRequestException("Start time and end time are required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("End time must be strictly after start time");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Price must be greater than or equal to 0");
        }
    }

    private void validateResourceAvailability(Resource resource) {
        if (!Boolean.TRUE.equals(resource.getAvailable())) {
            throw new BadRequestException("Resource '" + resource.getName() + "' is currently marked as unavailable");
        }
    }

    private void validateNoOverlappingReservation(Long resourceId, Long excludeReservationId, LocalDateTime startTime, LocalDateTime endTime) {
        boolean overlapping;
        if (excludeReservationId != null) {
            overlapping = reservationRepository.existsOverlappingReservationExcluding(
                    resourceId, excludeReservationId, ReservationStatus.CANCELLED, startTime, endTime
            );
        } else {
            overlapping = reservationRepository.existsOverlappingReservation(
                    resourceId, ReservationStatus.CANCELLED, startTime, endTime
            );
        }
        if (overlapping) {
            throw new ResourceConflictException("Resource is already reserved for the selected time window");
        }
    }

    private void validateReservationStatus(ReservationStatus status) {
        if (status == null) {
            throw new BadRequestException("Reservation status is required");
        }
    }

    private void validatePaginationParameters(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page index must not be less than zero");
        }
        if (size <= 0) {
            throw new BadRequestException("Page size must be greater than zero");
        }
    }

    private Sort buildValidatedSort(String[] sortParams) {
        if (sortParams == null || sortParams.length == 0) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        List<Sort.Order> orders = new ArrayList<>();

        if (sortParams.length == 2 && (sortParams[1].equalsIgnoreCase("asc") || sortParams[1].equalsIgnoreCase("desc"))) {
            String property = sortParams[0].trim();
            validateSortProperty(property);
            Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, property));
        } else {
            for (String param : sortParams) {
                if (param == null || param.isBlank()) continue;

                if (param.contains(",")) {
                    String[] parts = param.split(",");
                    String property = parts[0].trim();
                    validateSortProperty(property);
                    Sort.Direction direction = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                            ? Sort.Direction.DESC : Sort.Direction.ASC;
                    orders.add(new Sort.Order(direction, property));
                } else {
                    String property = param.trim();
                    validateSortProperty(property);
                    orders.add(new Sort.Order(Sort.Direction.ASC, property));
                }
            }
        }

        return orders.isEmpty() ? Sort.by(Sort.Direction.DESC, "createdAt") : Sort.by(orders);
    }

    private void validateSortProperty(String property) {
        if (!ALLOWED_SORT_FIELDS.contains(property)) {
            throw new BadRequestException(
                    "Invalid sort field: '" + property + "'. Allowed sort fields are: " + ALLOWED_SORT_FIELDS);
        }
    }

    public ReservationResponse mapToResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .resourceId(reservation.getResource().getId())
                .resourceName(reservation.getResource().getName())
                .resourceType(reservation.getResource().getType())
                .userId(reservation.getUser().getId())
                .username(reservation.getUser().getUsername())
                .userEmail(reservation.getUser().getEmail())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .price(reservation.getPrice())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}
