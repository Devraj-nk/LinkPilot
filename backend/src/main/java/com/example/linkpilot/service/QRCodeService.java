package com.example.linkpilot.service;

import com.example.linkpilot.dto.QRCodeRequest;
import com.example.linkpilot.exception.ForbiddenException;
import com.example.linkpilot.exception.ResourceNotFoundException;
import com.example.linkpilot.exception.UnsupportedFormatException;
import com.example.linkpilot.model.Link;
import com.example.linkpilot.model.QRCode;
import com.example.linkpilot.model.QRFormat;
import com.example.linkpilot.repository.QRCodeRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QRCodeService {

    private final QRCodeRepository qrCodeRepository;
    private final LinkService linkService;
    private final String publicBaseUrl;

    public QRCodeService(
            QRCodeRepository qrCodeRepository,
            LinkService linkService,
            @Value("${app.public-base-url}") String publicBaseUrl
    ) {
        this.qrCodeRepository = qrCodeRepository;
        this.linkService = linkService;
        this.publicBaseUrl = publicBaseUrl;
    }

    public List<QRCode> listForLink(UUID userId, UUID linkId) {
        linkService.getForUser(userId, linkId);
        return qrCodeRepository.findByLinkId(linkId);
    }

    @Transactional
    public QRCode create(UUID userId, UUID linkId, QRCodeRequest request) {
        Link link = linkService.getForUser(userId, linkId);

        QRCode qrCode = new QRCode();
        qrCode.setLink(link);
        if (request.format() != null) {
            qrCode.setFormat(request.format());
        }
        if (request.foregroundColor() != null) {
            qrCode.setForegroundColor(request.foregroundColor());
        }
        if (request.backgroundColor() != null) {
            qrCode.setBackgroundColor(request.backgroundColor());
        }
        if (request.size() != null) {
            qrCode.setSize(request.size());
        }
        return qrCodeRepository.save(qrCode);
    }

    @Transactional
    public void delete(UUID userId, UUID qrCodeId) {
        QRCode qrCode = qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new ResourceNotFoundException("QR code not found"));
        if (!qrCode.getLink().getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not own this QR code");
        }
        qrCodeRepository.delete(qrCode);
    }

    public byte[] renderImage(UUID qrCodeId) {
        QRCode qrCode = qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new ResourceNotFoundException("QR code not found"));

        if (qrCode.getFormat() != QRFormat.PNG) {
            throw new UnsupportedFormatException(qrCode.getFormat() + " rendering is not implemented yet, only PNG is supported");
        }

        String targetUrl = publicBaseUrl + "/" + qrCode.getLink().getShortCode();
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter().encode(
                    targetUrl, BarcodeFormat.QR_CODE, qrCode.getSize(), qrCode.getSize(), hints
            );

            MatrixToImageConfig config = new MatrixToImageConfig(
                    toArgb(qrCode.getForegroundColor()),
                    toArgb(qrCode.getBackgroundColor())
            );

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out, config);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to render QR code", e);
        }
    }

    private int toArgb(String hexColor) {
        return 0xFF000000 | Integer.parseInt(hexColor.replace("#", ""), 16);
    }
}
