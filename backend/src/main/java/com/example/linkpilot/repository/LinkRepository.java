package com.example.linkpilot.repository;

import com.example.linkpilot.model.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {
    Link findByShortCode(String shortCode);
}