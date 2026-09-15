package com.example.linkpilot.controller;

import com.example.linkpilot.dto.CampaignRequest;
import com.example.linkpilot.dto.CampaignResponse;
import com.example.linkpilot.dto.PageResponse;
import com.example.linkpilot.security.AuthenticatedUser;
import com.example.linkpilot.service.CampaignService;
import com.example.linkpilot.web.PageRequestFactory;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    public PageResponse<CampaignResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequestFactory.of(page, size, "createdAt");
        return PageResponse.from(campaignService.listForUser(principal.id(), pageable).map(CampaignResponse::from));
    }

    @GetMapping("/{id}")
    public CampaignResponse get(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return CampaignResponse.from(campaignService.getForUser(principal.id(), id));
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CampaignRequest request
    ) {
        CampaignResponse response = CampaignResponse.from(campaignService.create(principal.id(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public CampaignResponse update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody CampaignRequest request
    ) {
        return CampaignResponse.from(campaignService.update(principal.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        campaignService.delete(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
