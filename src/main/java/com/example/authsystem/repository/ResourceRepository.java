package com.example.authsystem.repository;

import com.example.authsystem.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Optional<Resource> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
