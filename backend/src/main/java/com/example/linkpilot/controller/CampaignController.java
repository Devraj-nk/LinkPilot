package com.example.linkpilot.controller;

import com.example.linkpilot.dto.CampaignRequest;
import com.example.linkpilot.dto.CampaignResponse;
import com.example.linkpilot.security.AuthenticatedUser;
import com.example.linkpilot.service.CampaignService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    public List<CampaignResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return campaignService.listForUser(principal.id()).stream()
                .map(CampaignResponse::from)
                .toList();
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
