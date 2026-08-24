package com.example.linkpilot.controller;

import com.example.linkpilot.dto.QRCodeRequest;
import com.example.linkpilot.dto.QRCodeResponse;
import com.example.linkpilot.security.AuthenticatedUser;
import com.example.linkpilot.service.QRCodeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class QRCodeController {

    private final QRCodeService qrCodeService;

    public QRCodeController(QRCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/api/links/{linkId}/qrcodes")
    public List<QRCodeResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID linkId
    ) {
        return qrCodeService.listForLink(principal.id(), linkId).stream()
                .map(QRCodeResponse::from)
                .toList();
    }

    @PostMapping("/api/links/{linkId}/qrcodes")
    public ResponseEntity<QRCodeResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID linkId,
            @Valid @RequestBody QRCodeRequest request
    ) {
        QRCodeResponse response = QRCodeResponse.from(qrCodeService.create(principal.id(), linkId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/api/qrcodes/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        qrCodeService.delete(principal.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/api/qrcodes/{id}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> image(@PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qrCodeService.renderImage(id));
    }
}
