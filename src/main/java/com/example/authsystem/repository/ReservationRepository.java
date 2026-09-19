package com.example.authsystem.repository;

import com.example.authsystem.entity.Reservation;
import com.example.authsystem.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.resource.id = :resourceId " +
           "AND r.status <> :cancelledStatus " +
           "AND r.startTime < :endTime AND r.endTime > :startTime")
    boolean existsOverlappingReservation(
            @Param("resourceId") Long resourceId,
            @Param("cancelledStatus") ReservationStatus cancelledStatus,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.resource.id = :resourceId " +
           "AND r.id <> :excludeReservationId " +
           "AND r.status <> :cancelledStatus " +
           "AND r.startTime < :endTime AND r.endTime > :startTime")
    boolean existsOverlappingReservationExcluding(
            @Param("resourceId") Long resourceId,
            @Param("excludeReservationId") Long excludeReservationId,
            @Param("cancelledStatus") ReservationStatus cancelledStatus,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
