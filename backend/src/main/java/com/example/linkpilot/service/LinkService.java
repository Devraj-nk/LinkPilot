package com.example.linkpilot.service;

import com.example.linkpilot.dto.LinkRequest;
import com.example.linkpilot.exception.DuplicateResourceException;
import com.example.linkpilot.exception.ForbiddenException;
import com.example.linkpilot.exception.GoneException;
import com.example.linkpilot.exception.ResourceNotFoundException;
import com.example.linkpilot.model.Campaign;
import com.example.linkpilot.model.Domain;
import com.example.linkpilot.model.DomainVerificationStatus;
import com.example.linkpilot.model.Link;
import com.example.linkpilot.model.LinkStatus;
import com.example.linkpilot.repository.CampaignRepository;
import com.example.linkpilot.repository.DomainRepository;
import com.example.linkpilot.repository.LinkRepository;
import com.example.linkpilot.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LinkService {

    private final LinkRepository linkRepository;
    private final CampaignRepository campaignRepository;
    private final DomainRepository domainRepository;
    private final UserRepository userRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final RedisTemplate<String, String> redisTemplate;

    public LinkService(
            LinkRepository linkRepository,
            CampaignRepository campaignRepository,
            DomainRepository domainRepository,
            UserRepository userRepository,
            ShortCodeGenerator shortCodeGenerator,
            RedisTemplate<String, String> redisTemplate
    ) {
        this.linkRepository = linkRepository;
        this.campaignRepository = campaignRepository;
        this.domainRepository = domainRepository;
        this.userRepository = userRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.redisTemplate = redisTemplate;
    }

    public Page<Link> listForUser(UUID userId, UUID campaignId, Pageable pageable) {
        return campaignId != null
                ? linkRepository.findByUserIdAndCampaignId(userId, campaignId, pageable)
                : linkRepository.findByUserId(userId, pageable);
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
        link.setDomain(resolveDomain(userId, request.domainId()));
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
        link.setDomain(resolveDomain(userId, request.domainId()));
        link.setOriginalUrl(request.originalUrl());
        link.setTitle(request.title());
        link.setExpiresAt(request.expiresAt());
        link = linkRepository.save(link);

        cache(link);
        return link;
    }

    @Transactional
    public Link updateStatus(UUID userId, UUID linkId, LinkStatus status) {
        Link link = getForUser(userId, linkId);
        link.setStatus(status);
        return linkRepository.save(link);
    }

    @Transactional
    public void delete(UUID userId, UUID linkId) {
        Link link = getForUser(userId, linkId);
        linkRepository.delete(link);
        redisTemplate.delete(link.getShortCode());
    }

    /**
     * Resolves a link for the public redirect. `host` is the incoming request's Host
     * header: a link bound to a custom domain only resolves on that domain - anywhere
     * else, it's treated as not-found rather than leaking that the code exists elsewhere.
     * A link with no domain resolves on any host (the default, unscoped behavior).
     */
    @Transactional
    public Link resolve(String shortCode, String host) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));

        if (link.getDomain() != null && (host == null || !hostMatches(host, link.getDomain().getDomain()))) {
            throw new ResourceNotFoundException("Link not found");
        }
        if (link.getStatus() != LinkStatus.ACTIVE) {
            throw new GoneException("This link is no longer active");
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new GoneException("This link has expired");
        }

        link.setClickCount(link.getClickCount() + 1);
        return linkRepository.save(link);
    }

    private boolean hostMatches(String requestHost, String domain) {
        String normalizedHost = requestHost.split(":")[0].toLowerCase();
        return normalizedHost.equalsIgnoreCase(domain);
    }

    private Campaign resolveCampaign(UUID userId, UUID campaignId) {
        if (campaignId == null) {
            return null;
        }
        return campaignRepository.findByIdAndUserId(campaignId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
    }

    private Domain resolveDomain(UUID userId, UUID domainId) {
        if (domainId == null) {
            return null;
        }
        Domain domain = domainRepository.findByIdAndUserId(domainId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found"));
        if (domain.getVerificationStatus() != DomainVerificationStatus.VERIFIED) {
            throw new ForbiddenException("Domain must be verified before it can be used on a link");
        }
        return domain;
    }

    private void cache(Link link) {
        redisTemplate.opsForValue().set(link.getShortCode(), link.getOriginalUrl(), 1, TimeUnit.HOURS);
    }
}
