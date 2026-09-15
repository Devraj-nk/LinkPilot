package com.example.linkpilot.controller;

import com.example.linkpilot.dto.DomainRequest;
import com.example.linkpilot.dto.DomainResponse;
import com.example.linkpilot.dto.PageResponse;
import com.example.linkpilot.security.AuthenticatedUser;
import com.example.linkpilot.service.DomainService;
import com.example.linkpilot.web.PageRequestFactory;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/domains")
public class DomainController {

    private final DomainService domainService;

    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    @GetMapping
    public PageResponse<DomainResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequestFactory.of(page, size, "createdAt");
        return PageResponse.from(domainService.listForUser(principal.id(), pageable).map(DomainResponse::from));
    }

    @PostMapping
    public ResponseEntity<DomainResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody DomainRequest request
    ) {
        DomainResponse response = DomainResponse.from(domainService.create(principal.id(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/verify")
    public DomainResponse verify(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return DomainResponse.from(domainService.verify(principal.id(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        domainService.delete(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
