package com.example.linkpilot.service;

import com.example.linkpilot.dto.APIKeyRequest;
import com.example.linkpilot.exception.ResourceNotFoundException;
import com.example.linkpilot.model.APIKey;
import com.example.linkpilot.repository.APIKeyRepository;
import com.example.linkpilot.repository.UserRepository;
import com.example.linkpilot.security.TokenHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class APIKeyService {

    public record CreatedApiKey(APIKey entity, String rawKey) {
    }

    private final APIKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final TokenHasher tokenHasher;

    public APIKeyService(APIKeyRepository apiKeyRepository, UserRepository userRepository, TokenHasher tokenHasher) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.tokenHasher = tokenHasher;
    }

    public List<APIKey> listForUser(UUID userId) {
        return apiKeyRepository.findByUserId(userId);
    }

    @Transactional
    public CreatedApiKey create(UUID userId, APIKeyRequest request) {
        String rawKey = "lp_" + tokenHasher.generateRawToken();

        APIKey apiKey = new APIKey();
        apiKey.setUser(userRepository.getReferenceById(userId));
        apiKey.setName(request.name());
        apiKey.setKeyHash(tokenHasher.hash(rawKey));
        apiKey.setExpiresAt(request.expiresAt());
        apiKey = apiKeyRepository.save(apiKey);

        return new CreatedApiKey(apiKey, rawKey);
    }

    @Transactional
    public void delete(UUID userId, UUID apiKeyId) {
        APIKey apiKey = apiKeyRepository.findByIdAndUserId(apiKeyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found"));
        apiKeyRepository.delete(apiKey);
    }
}
