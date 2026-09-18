package com.example.linkpilot.controller;

import com.example.linkpilot.exception.RateLimitExceededException;
import com.example.linkpilot.model.Link;
import com.example.linkpilot.security.RateLimiterService;
import com.example.linkpilot.service.ClickEventService;
import com.example.linkpilot.service.LinkService;
import com.example.linkpilot.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
public class RedirectController {

    // Keyed by IP: the thing being guarded against is a bot hammering the redirect
    // endpoint itself (a cheap way to point traffic at an arbitrary destination URL
    // through this service), not any one short link - legitimate traffic for a single
    // popular link is expected to come from many different IPs, not one.
    private static final int REDIRECT_LIMIT = 60;
    private static final Duration REDIRECT_WINDOW = Duration.ofMinutes(1);

    private final LinkService linkService;
    private final ClickEventService clickEventService;
    private final RateLimiterService rateLimiter;

    public RedirectController(LinkService linkService, ClickEventService clickEventService, RateLimiterService rateLimiter) {
        this.linkService = linkService;
        this.clickEventService = clickEventService;
        this.rateLimiter = rateLimiter;
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
        String clientIp = ClientIpResolver.resolve(request);
        if (!rateLimiter.allow("ratelimit:redirect:" + clientIp, REDIRECT_LIMIT, REDIRECT_WINDOW)) {
            throw new RateLimitExceededException("Too many requests - try again shortly");
        }

        String effectiveHost = (host != null && !host.isBlank()) ? host : request.getHeader("Host");
        Link link = linkService.resolve(shortCode, effectiveHost);

        // Read everything off the request now, on this thread - recordAsync runs on a
        // separate thread pool after this method returns, and Tomcat is free to recycle
        // this Request object as soon as the response is committed. A recycled request
        // throws on any getXxx() call, which silently dropped click events under real
        // latency (a slow-enough analytics store gives Tomcat time to recycle before the
        // async thread gets to it) - passing the live request into the async method was
        // the bug, not the async-ness itself.
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");
        clickEventService.recordAsync(link, userAgent, referer, clientIp);

        return ResponseEntity.status(302).header("Location", link.getOriginalUrl()).build();
    }
}
