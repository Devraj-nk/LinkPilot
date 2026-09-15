package com.example.linkpilot.repository;

import com.example.linkpilot.model.Domain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DomainRepository extends JpaRepository<Domain, UUID> {
    Page<Domain> findByUserId(UUID userId, Pageable pageable);
    Optional<Domain> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByDomain(String domain);
}
