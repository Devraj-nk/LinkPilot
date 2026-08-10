package com.example.linkpilot.controller;

import com.example.linkpilot.model.Link;
import com.example.linkpilot.service.LinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/links")
public class LinkController {

    @Autowired
    private LinkService linkService;

    @PostMapping
    public ResponseEntity<Link> createLink(@RequestParam String url) {
        Link link = linkService.createLink(url);
        return ResponseEntity.ok(link);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> getLink(@PathVariable String shortCode) {
        Optional<Link> linkOptional = linkService.getLinkByShortCode(shortCode);
        if (linkOptional.isPresent()) {
            Link link = linkOptional.get();
            // Increment click count when the link is accessed
            linkService.incrementClickCount(shortCode);
            // Redirect to the original URL
            return ResponseEntity.status(302).header("Location", link.getOriginalUrl()).build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}