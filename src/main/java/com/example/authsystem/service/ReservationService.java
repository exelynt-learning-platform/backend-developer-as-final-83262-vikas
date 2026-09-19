package com.example.authsystem.service;

import com.example.authsystem.dto.common.PagedResponse;
import com.example.authsystem.dto.reservation.ReservationRequest;
import com.example.authsystem.dto.reservation.ReservationResponse;
import com.example.authsystem.dto.reservation.ReservationUpdateRequest;
import com.example.authsystem.entity.*;
import com.example.authsystem.exception.AccessDeniedCustomException;
import com.example.authsystem.exception.BadRequestException;
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

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request, CustomUserDetails currentUser) {
        validateReservationTimes(request.getStartTime(), request.getEndTime());
        validatePrice(request.getPrice());

        // Always resolve user strictly from authenticated SecurityContext / JWT
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database"));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + request.getResourceId()));

        if (!Boolean.TRUE.equals(resource.getAvailable())) {
            throw new BadRequestException("Resource '" + resource.getName() + "' is currently marked as unavailable");
        }

        ReservationStatus initialStatus = request.getStatus() != null ? request.getStatus() : ReservationStatus.PENDING;

        Reservation reservation = Reservation.builder()
                .user(user)
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

        if (page < 0) {
            throw new BadRequestException("Page index must not be less than zero");
        }
        if (size <= 0) {
            throw new BadRequestException("Page size must be greater than zero");
        }

        Sort sort = buildValidatedSort(sortParams);
        Pageable pageable = PageRequest.of(page, size, sort);

        // RBAC: If USER, restrict query to only their own reservations.
        // If ADMIN, allow querying across all reservations.
        Long targetUserId = (currentUser.getRole() == Role.ADMIN) ? null : currentUser.getId();

        Specification<Reservation> spec = ReservationSpecification.filterReservations(
                targetUserId, status, minPrice, maxPrice
        );

        Page<ReservationResponse> reservationPage = reservationRepository.findAll(spec, pageable)
                .map(this::mapToResponse);

        return PagedResponse.of(reservationPage);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id, CustomUserDetails currentUser) {
        Reservation reservation = findReservationById(id);
        enforceOwnershipOrAdmin(reservation, currentUser);
        return mapToResponse(reservation);
    }

    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationUpdateRequest request, CustomUserDetails currentUser) {
        Reservation reservation = findReservationById(id);
        enforceOwnershipOrAdmin(reservation, currentUser);

        validateReservationTimes(request.getStartTime(), request.getEndTime());
        validatePrice(request.getPrice());

        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());
        if (request.getStatus() != null) {
            reservation.setStatus(request.getStatus());
        }

        Reservation updated = reservationRepository.save(reservation);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteReservation(Long id, CustomUserDetails currentUser) {
        Reservation reservation = findReservationById(id);
        enforceOwnershipOrAdmin(reservation, currentUser);
        reservationRepository.delete(reservation);
    }

    private Reservation findReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + id));
    }

    private void enforceOwnershipOrAdmin(Reservation reservation, CustomUserDetails currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return; // ADMIN has full access
        }
        if (!reservation.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedCustomException("You do not have permission to access another user's reservation");
        }
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

    private Sort buildValidatedSort(String[] sortParams) {
        if (sortParams == null || sortParams.length == 0) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        java.util.List<Sort.Order> orders = new java.util.ArrayList<>();

        // Spring MVC splits ?sort=price,desc into String[] {"price", "desc"}
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
