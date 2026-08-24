package com.example.linkpilot.repository;

import com.example.linkpilot.model.APIKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface APIKeyRepository extends JpaRepository<APIKey, UUID> {
    List<APIKey> findByUserId(UUID userId);
    Optional<APIKey> findByIdAndUserId(UUID id, UUID userId);
}
