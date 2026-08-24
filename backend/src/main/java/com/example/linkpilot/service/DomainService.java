package com.example.linkpilot.service;

import com.example.linkpilot.dto.DomainRequest;
import com.example.linkpilot.exception.DuplicateResourceException;
import com.example.linkpilot.exception.ResourceNotFoundException;
import com.example.linkpilot.model.Domain;
import com.example.linkpilot.model.DomainVerificationStatus;
import com.example.linkpilot.repository.DomainRepository;
import com.example.linkpilot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DomainService {

    private final DomainRepository domainRepository;
    private final UserRepository userRepository;

    public DomainService(DomainRepository domainRepository, UserRepository userRepository) {
        this.domainRepository = domainRepository;
        this.userRepository = userRepository;
    }

    public List<Domain> listForUser(UUID userId) {
        return domainRepository.findByUserId(userId);
    }

    public Domain getForUser(UUID userId, UUID domainId) {
        return domainRepository.findByIdAndUserId(domainId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found"));
    }

    @Transactional
    public Domain create(UUID userId, DomainRequest request) {
        if (domainRepository.existsByDomain(request.domain())) {
            throw new DuplicateResourceException("This domain is already registered");
        }
        Domain domain = new Domain();
        domain.setUser(userRepository.getReferenceById(userId));
        domain.setDomain(request.domain());
        return domainRepository.save(domain);
    }

    @Transactional
    public void delete(UUID userId, UUID domainId) {
        Domain domain = getForUser(userId, domainId);
        domainRepository.delete(domain);
    }

    @Transactional
    public Domain verify(UUID userId, UUID domainId) {
        // Stub: a real implementation would check a DNS TXT record before flipping this.
        Domain domain = getForUser(userId, domainId);
        domain.setVerificationStatus(DomainVerificationStatus.VERIFIED);
        domain.setVerifiedAt(OffsetDateTime.now());
        return domainRepository.save(domain);
    }
}
