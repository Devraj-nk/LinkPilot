package com.example.linkpilot.controller;

import com.example.linkpilot.dto.APIKeyCreateResponse;
import com.example.linkpilot.dto.APIKeyRequest;
import com.example.linkpilot.dto.APIKeyResponse;
import com.example.linkpilot.security.AuthenticatedUser;
import com.example.linkpilot.service.APIKeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/api-keys")
public class APIKeyController {

    private final APIKeyService apiKeyService;

    public APIKeyController(APIKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public List<APIKeyResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return apiKeyService.listForUser(principal.id()).stream()
                .map(APIKeyResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<APIKeyCreateResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody APIKeyRequest request
    ) {
        var created = apiKeyService.create(principal.id(), request);
        var response = new APIKeyCreateResponse(created.entity().getId(), created.entity().getName(), created.rawKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        apiKeyService.delete(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
