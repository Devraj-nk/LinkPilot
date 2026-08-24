package com.example.linkpilot.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "qr_codes")
public class QRCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QRFormat format = QRFormat.PNG;

    @Column(name = "foreground_color", nullable = false, length = 7)
    private String foregroundColor = "#000000";

    @Column(name = "background_color", nullable = false, length = 7)
    private String backgroundColor = "#FFFFFF";

    @Column(nullable = false)
    private int size = 256;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }

    public QRFormat getFormat() {
        return format;
    }

    public void setFormat(QRFormat format) {
        this.format = format;
    }

    public String getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(String foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
