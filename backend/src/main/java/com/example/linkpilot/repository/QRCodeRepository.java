package com.example.linkpilot.repository;

import com.example.linkpilot.model.QRCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QRCodeRepository extends JpaRepository<QRCode, UUID> {
    List<QRCode> findByLinkId(UUID linkId);
}
