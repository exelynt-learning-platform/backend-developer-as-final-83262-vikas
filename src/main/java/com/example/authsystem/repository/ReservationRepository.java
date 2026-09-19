package com.example.authsystem.repository;

import com.example.authsystem.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    Page<Reservation> findByResourceId(Long resourceId, Pageable pageable);
}
