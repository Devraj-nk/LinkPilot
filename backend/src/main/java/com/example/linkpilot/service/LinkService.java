package com.example.linkpilot.service;

import com.example.linkpilot.dto.LinkRequest;
import com.example.linkpilot.exception.DuplicateResourceException;
import com.example.linkpilot.exception.GoneException;
import com.example.linkpilot.exception.ResourceNotFoundException;
import com.example.linkpilot.model.Campaign;
import com.example.linkpilot.model.Link;
import com.example.linkpilot.model.LinkStatus;
import com.example.linkpilot.repository.CampaignRepository;
import com.example.linkpilot.repository.LinkRepository;
import com.example.linkpilot.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LinkService {

    private final LinkRepository linkRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final RedisTemplate<String, String> redisTemplate;

    public LinkService(
            LinkRepository linkRepository,
            CampaignRepository campaignRepository,
            UserRepository userRepository,
            ShortCodeGenerator shortCodeGenerator,
            RedisTemplate<String, String> redisTemplate
    ) {
        this.linkRepository = linkRepository;
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.redisTemplate = redisTemplate;
    }

    public List<Link> listForUser(UUID userId, UUID campaignId) {
        return campaignId != null
                ? linkRepository.findByUserIdAndCampaignId(userId, campaignId)
                : linkRepository.findByUserId(userId);
    }

    public Link getForUser(UUID userId, UUID linkId) {
        return linkRepository.findByIdAndUserId(linkId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));
    }

    @Transactional
    public Link create(UUID userId, LinkRequest request) {
        String shortCode;
        if (request.customCode() != null && !request.customCode().isBlank()) {
            if (linkRepository.existsByShortCode(request.customCode())) {
                throw new DuplicateResourceException("Short code already in use");
            }
            shortCode = request.customCode();
        } else {
            shortCode = shortCodeGenerator.generate();
        }

        Link link = new Link();
        link.setUser(userRepository.getReferenceById(userId));
        link.setCampaign(resolveCampaign(userId, request.campaignId()));
        link.setShortCode(shortCode);
        link.setOriginalUrl(request.originalUrl());
        link.setTitle(request.title());
        link.setExpiresAt(request.expiresAt());
        link = linkRepository.save(link);

        cache(link);
        return link;
    }

    @Transactional
    public Link update(UUID userId, UUID linkId, LinkRequest request) {
        Link link = getForUser(userId, linkId);
        link.setCampaign(resolveCampaign(userId, request.campaignId()));
        link.setOriginalUrl(request.originalUrl());
        link.setTitle(request.title());
        link.setExpiresAt(request.expiresAt());
        link = linkRepository.save(link);

        cache(link);
        return link;
    }

    @Transactional
    public void delete(UUID userId, UUID linkId) {
        Link link = getForUser(userId, linkId);
        linkRepository.delete(link);
        redisTemplate.delete(link.getShortCode());
    }

    @Transactional
    public Link resolve(String shortCode) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));

        if (link.getStatus() != LinkStatus.ACTIVE) {
            throw new GoneException("This link is no longer active");
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new GoneException("This link has expired");
        }

        link.setClickCount(link.getClickCount() + 1);
        return linkRepository.save(link);
    }

    private Campaign resolveCampaign(UUID userId, UUID campaignId) {
        if (campaignId == null) {
            return null;
        }
        return campaignRepository.findByIdAndUserId(campaignId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
    }

    private void cache(Link link) {
        redisTemplate.opsForValue().set(link.getShortCode(), link.getOriginalUrl(), 1, TimeUnit.HOURS);
    }
}
