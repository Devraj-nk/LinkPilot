package com.example.linkpilot.service;

import com.example.linkpilot.dto.CampaignRequest;
import com.example.linkpilot.exception.ResourceNotFoundException;
import com.example.linkpilot.model.Campaign;
import com.example.linkpilot.model.CampaignStatus;
import com.example.linkpilot.repository.CampaignRepository;
import com.example.linkpilot.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;

    public CampaignService(CampaignRepository campaignRepository, UserRepository userRepository) {
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
    }

    public Page<Campaign> listForUser(UUID userId, Pageable pageable) {
        return campaignRepository.findByUserId(userId, pageable);
    }

    public Campaign getForUser(UUID userId, UUID campaignId) {
        return campaignRepository.findByIdAndUserId(campaignId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
    }

    @Transactional
    public Campaign create(UUID userId, CampaignRequest request) {
        Campaign campaign = new Campaign();
        campaign.setUser(userRepository.getReferenceById(userId));
        campaign.setName(request.name());
        campaign.setDescription(request.description());
        if (request.status() != null) {
            campaign.setStatus(request.status());
        }
        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign update(UUID userId, UUID campaignId, CampaignRequest request) {
        Campaign campaign = getForUser(userId, campaignId);
        campaign.setName(request.name());
        campaign.setDescription(request.description());
        if (request.status() != null) {
            campaign.setStatus(request.status());
        }
        return campaignRepository.save(campaign);
    }

    @Transactional
    public void delete(UUID userId, UUID campaignId) {
        Campaign campaign = getForUser(userId, campaignId);
        campaignRepository.delete(campaign);
    }
}
