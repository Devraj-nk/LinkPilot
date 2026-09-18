package com.example.linkpilot.security;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Fixed-window request counter backed by Redis: INCR a per-window key, set it to expire
 * on first use so the window resets on its own, and reject once the count exceeds the
 * limit. Simple and easy to reason about - good enough at this project's traffic; a
 * sliding-window or token-bucket scheme would smooth out edge-of-window bursts, but
 * isn't worth the extra complexity here.
 *
 * Fails open (allows the request) on any Redis error, including the counter coming back
 * null - Redis is also just a cache for the redirect path elsewhere in this app, and a
 * rate limiter outage blocking real traffic would be a worse failure than a brief window
 * of being unprotected.
 */
@Component
public class RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;

    public RateLimiterService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean allow(String key, int limit, Duration window) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                return true;
            }
            if (count == 1L) {
                redisTemplate.expire(key, window);
            }
            return count <= limit;
        } catch (Exception e) {
            return true;
        }
    }
}
