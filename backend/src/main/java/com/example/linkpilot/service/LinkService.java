package com.example.linkpilot.service;

import com.example.linkpilot.model.Link;
import com.example.linkpilot.repository.LinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class LinkService {

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public Link createLink(String originalUrl) {
        // Generate a short code (simplified for example)
        String shortCode = java.util.UUID.randomUUID().toString().substring(0, 8);
        Link link = new Link();
        link.setOriginalUrl(originalUrl);
        link.setShortCode(shortCode);
        link.setClickCount(0);
        link = linkRepository.save(link);

        // Cache the link in Redis for quick access
        redisTemplate.opsForValue().set(shortCode, originalUrl, 1, TimeUnit.HOURS);
        return link;
    }

    public Optional<Link> getLinkByShortCode(String shortCode) {
        // Try to get from cache first
        String cachedUrl = redisTemplate.opsForValue().get(shortCode);
        if (cachedUrl != null) {
            // If found in cache, we can return a Link object (without hitting DB)
            // For simplicity, we'll return a Link object with the cached URL and shortCode.
            // In a real app, you might want to get the full object from DB or store more in Redis.
            Link link = new Link();
            link.setShortCode(shortCode);
            link.setOriginalUrl(cachedUrl);
            return Optional.of(link);
        }

        // If not in cache, get from DB
        Optional<Link> linkOptional = linkRepository.findByShortCode(shortCode);
        linkOptional.ifPresent(link -> {
            // Cache the result for future requests
            redisTemplate.opsForValue().set(shortCode, link.getOriginalUrl(), 1, TimeUnit.HOURS);
        });
        return linkOptional;
    }

    public void incrementClickCount(String shortCode) {
        linkRepository.findByShortCode(shortCode).ifPresent(link -> {
            link.setClickCount(link.getClickCount() + 1);
            linkRepository.save(link);
            // Update cache if needed (we are not caching the click count, but we could)
        });
    }
}