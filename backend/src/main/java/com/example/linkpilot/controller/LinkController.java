package com.example.linkpilot.controller;

import com.example.linkpilot.dto.LinkRequest;
import com.example.linkpilot.dto.LinkResponse;
import com.example.linkpilot.dto.LinkStatusRequest;
import com.example.linkpilot.dto.PageResponse;
import com.example.linkpilot.exception.RateLimitExceededException;
import com.example.linkpilot.security.AuthenticatedUser;
import com.example.linkpilot.security.RateLimiterService;
import com.example.linkpilot.service.LinkService;
import com.example.linkpilot.web.PageRequestFactory;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/links")
public class LinkController {

    // Keyed by user, not IP - the caller is already authenticated here, and the thing
    // being protected against is one account scripting mass link creation, not the
    // network address it happens to come from.
    private static final int CREATE_LIMIT = 30;
    private static final Duration CREATE_WINDOW = Duration.ofMinutes(1);

    private final LinkService linkService;
    private final String publicBaseUrl;
    private final RateLimiterService rateLimiter;

    public LinkController(
            LinkService linkService,
            @Value("${app.public-base-url}") String publicBaseUrl,
            RateLimiterService rateLimiter
    ) {
        this.linkService = linkService;
        this.publicBaseUrl = publicBaseUrl;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    public PageResponse<LinkResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) UUID campaignId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequestFactory.of(page, size, "createdAt");
        Page<LinkResponse> links = linkService.listForUser(principal.id(), campaignId, pageable)
                .map(link -> LinkResponse.from(link, publicBaseUrl));
        return PageResponse.from(links);
    }

    @GetMapping("/{id}")
    public LinkResponse get(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return LinkResponse.from(linkService.getForUser(principal.id(), id), publicBaseUrl);
    }

    @PostMapping
    public ResponseEntity<LinkResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody LinkRequest request
    ) {
        if (!rateLimiter.allow("ratelimit:create-link:" + principal.id(), CREATE_LIMIT, CREATE_WINDOW)) {
            throw new RateLimitExceededException("Too many links created - slow down and try again shortly");
        }
        LinkResponse response = LinkResponse.from(linkService.create(principal.id(), request), publicBaseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public LinkResponse update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody LinkRequest request
    ) {
        return LinkResponse.from(linkService.update(principal.id(), id, request), publicBaseUrl);
    }

    @PatchMapping("/{id}/status")
    public LinkResponse updateStatus(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody LinkStatusRequest request
    ) {
        return LinkResponse.from(linkService.updateStatus(principal.id(), id, request.status()), publicBaseUrl);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        linkService.delete(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
