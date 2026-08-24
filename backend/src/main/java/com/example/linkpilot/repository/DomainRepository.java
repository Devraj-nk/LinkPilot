package com.example.linkpilot.repository;

import com.example.linkpilot.model.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DomainRepository extends JpaRepository<Domain, UUID> {
    List<Domain> findByUserId(UUID userId);
    Optional<Domain> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByDomain(String domain);
}
