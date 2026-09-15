package com.example.linkpilot.controller;

import com.example.linkpilot.dto.APIKeyCreateResponse;
import com.example.linkpilot.dto.APIKeyRequest;
import com.example.linkpilot.dto.APIKeyResponse;
import com.example.linkpilot.dto.PageResponse;
import com.example.linkpilot.security.AuthenticatedUser;
import com.example.linkpilot.service.APIKeyService;
import com.example.linkpilot.web.PageRequestFactory;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/api-keys")
public class APIKeyController {

    private final APIKeyService apiKeyService;

    public APIKeyController(APIKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public PageResponse<APIKeyResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequestFactory.of(page, size, "createdAt");
        return PageResponse.from(apiKeyService.listForUser(principal.id(), pageable).map(APIKeyResponse::from));
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
