package com.example.linkpilot.controller;

import com.example.linkpilot.dto.LinkRequest;
import com.example.linkpilot.dto.LinkResponse;
import com.example.linkpilot.security.AuthenticatedUser;
import com.example.linkpilot.service.LinkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/links")
public class LinkController {

    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }

    @GetMapping
    public List<LinkResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) UUID campaignId
    ) {
        return linkService.listForUser(principal.id(), campaignId).stream()
                .map(LinkResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public LinkResponse get(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return LinkResponse.from(linkService.getForUser(principal.id(), id));
    }

    @PostMapping
    public ResponseEntity<LinkResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody LinkRequest request
    ) {
        LinkResponse response = LinkResponse.from(linkService.create(principal.id(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public LinkResponse update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody LinkRequest request
    ) {
        return LinkResponse.from(linkService.update(principal.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        linkService.delete(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
