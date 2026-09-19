package com.example.authsystem.repository;

import com.example.authsystem.entity.Reservation;
import com.example.authsystem.entity.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReservationSpecificationTest {

    @Test
    @DisplayName("Should build specification with all parameters provided")
    void testFilterWithAllParameters() {
        Specification<Reservation> spec = ReservationSpecification.filterReservations(
                1L, ReservationStatus.PENDING, BigDecimal.valueOf(50), BigDecimal.valueOf(200)
        );
        assertNotNull(spec);
    }

    @Test
    @DisplayName("Should build specification with null parameters (admin view, all records)")
    void testFilterWithNullParameters() {
        Specification<Reservation> spec = ReservationSpecification.filterReservations(
                null, null, null, null
        );
        assertNotNull(spec);
    }
}
