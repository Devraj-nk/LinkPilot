package com.example.linkpilot.repository;

import com.example.linkpilot.model.APIKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface APIKeyRepository extends JpaRepository<APIKey, UUID> {
    Page<APIKey> findByUserId(UUID userId, Pageable pageable);
    Optional<APIKey> findByIdAndUserId(UUID id, UUID userId);
}
