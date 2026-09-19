package com.example.authsystem.repository;

import com.example.authsystem.entity.Reservation;
import com.example.authsystem.entity.ReservationStatus;
import com.example.authsystem.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReservationSpecification {

    /**
     * Constructs a dynamic query specification for reservations.
     * Incorporates direct database JOIN authorization for user tenant filtering,
     * avoiding in-memory owner ID collections or large SQL IN-clauses.
     */
    public static Specification<Reservation> filterReservations(
            Long userId,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Direct SQL JOIN authorization directly within the query predicate
            if (userId != null) {
                Join<Reservation, User> userJoin = root.join("user", JoinType.INNER);
                predicates.add(criteriaBuilder.equal(userJoin.get("id"), userId));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
