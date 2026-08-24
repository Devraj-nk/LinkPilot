package com.example.linkpilot.dto;

import com.example.linkpilot.model.QRFormat;

public record QRCodeRequest(
        QRFormat format,
        String foregroundColor,
        String backgroundColor,
        Integer size
) {
}
