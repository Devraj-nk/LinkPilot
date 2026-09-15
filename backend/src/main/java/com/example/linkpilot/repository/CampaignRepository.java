package com.example.linkpilot.repository;

import com.example.linkpilot.model.Campaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    Page<Campaign> findByUserId(UUID userId, Pageable pageable);
    Optional<Campaign> findByIdAndUserId(UUID id, UUID userId);
}
