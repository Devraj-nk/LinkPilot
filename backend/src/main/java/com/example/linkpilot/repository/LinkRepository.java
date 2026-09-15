package com.example.linkpilot.repository;

import com.example.linkpilot.model.Link;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkRepository extends JpaRepository<Link, UUID> {
    Optional<Link> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
    Optional<Link> findByIdAndUserId(UUID id, UUID userId);
    Page<Link> findByUserId(UUID userId, Pageable pageable);
    Page<Link> findByUserIdAndCampaignId(UUID userId, UUID campaignId, Pageable pageable);
}
