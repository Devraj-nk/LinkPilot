package com.example.linkpilot.dto;

import com.example.linkpilot.model.QRCode;
import com.example.linkpilot.model.QRFormat;

import java.time.OffsetDateTime;
import java.util.UUID;

public record QRCodeResponse(
        UUID id,
        UUID linkId,
        QRFormat format,
        String foregroundColor,
        String backgroundColor,
        int size,
        OffsetDateTime createdAt,
        String imageUrl
) {
    public static QRCodeResponse from(QRCode qrCode) {
        return new QRCodeResponse(
                qrCode.getId(),
                qrCode.getLink().getId(),
                qrCode.getFormat(),
                qrCode.getForegroundColor(),
                qrCode.getBackgroundColor(),
                qrCode.getSize(),
                qrCode.getCreatedAt(),
                "/api/qrcodes/" + qrCode.getId() + "/image"
        );
    }
}
