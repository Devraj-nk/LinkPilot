package com.example.linkpilot.controller;

import com.example.linkpilot.model.Link;
import com.example.linkpilot.service.LinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private final LinkService linkService;

    public RedirectController(LinkService linkService) {
        this.linkService = linkService;
    }

    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        Link link = linkService.resolve(shortCode);
        return ResponseEntity.status(302).header("Location", link.getOriginalUrl()).build();
    }
}
