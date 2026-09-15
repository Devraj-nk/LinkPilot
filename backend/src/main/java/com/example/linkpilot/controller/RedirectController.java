package com.example.linkpilot.controller;

import com.example.linkpilot.model.Link;
import com.example.linkpilot.service.ClickEventService;
import com.example.linkpilot.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private final LinkService linkService;
    private final ClickEventService clickEventService;

    public RedirectController(LinkService linkService, ClickEventService clickEventService) {
        this.linkService = linkService;
        this.clickEventService = clickEventService;
    }

    /**
     * `host` carries the domain the browser actually visited. It's populated by the
     * Next.js frontend's `[shortCode]` route, which is the real entry point for a short
     * link (e.g. `go.example.com/abc123`) - by the time that route 307s the browser here,
     * the request's own `Host` header is this server's address, not the domain the user
     * visited, so the original host has to travel as a query param instead. Falls back to
     * the request's `Host` header when absent, so hitting this endpoint directly (tests,
     * curl, API clients) still works.
     */
    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            @RequestParam(required = false) String host,
            HttpServletRequest request
    ) {
        String effectiveHost = (host != null && !host.isBlank()) ? host : request.getHeader("Host");
        Link link = linkService.resolve(shortCode, effectiveHost);
        clickEventService.recordAsync(link, request);
        return ResponseEntity.status(302).header("Location", link.getOriginalUrl()).build();
    }
}
