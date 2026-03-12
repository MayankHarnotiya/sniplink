package com.snip.link.service;

import com.snip.link.model.UrlMapping;
import com.snip.link.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UrlShortenerService {
    private final UrlMappingRepository repository;
    private final RedisTemplate<String, String> redisTemplate;
    private final HashService hashService;

    private static final long CACHE_TTL_HOURS = 24;
    private static final String REDIS_PREFIX = "url:";

    // ── SHORTEN ──────────────────────────────────────────────
    public String shortenUrl(String originalUrl) {
        String shortCode = generateUniqueCode(originalUrl);

        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode(shortCode);
        mapping.setOriginalUrl(originalUrl);
        mapping.setCreatedAt(LocalDateTime.now());
        mapping.setExpiresAt(LocalDateTime.now().plusDays(30)); // 30-day TTL
        mapping.setClickCount(0L);

        repository.save(mapping);

        // Also write to cache immediately ("write-through" caching)
        cacheUrl(shortCode, originalUrl);

        return shortCode;
    }

    // ── RESOLVE ───────────────────────────────────────────────
    public String resolveUrl(String shortCode) {
        // 1. Check Redis cache first (fast path)
        String cachedUrl = redisTemplate.opsForValue()
                .get(REDIS_PREFIX + shortCode);
        if (cachedUrl != null) {
            incrementClickCountAsync(shortCode); // don't slow down redirect
            return cachedUrl;
        }

        // 2. Cache miss → hit MySQL (slow path)
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("Short URL not found"));

        // Check expiry
        if (mapping.getExpiresAt() != null &&
                LocalDateTime.now().isAfter(mapping.getExpiresAt())) {
            throw new RuntimeException("This short URL has expired");
        }

        // 3. Re-populate cache for future requests
        cacheUrl(shortCode, mapping.getOriginalUrl());

        incrementClickCountAsync(shortCode);
        return mapping.getOriginalUrl();
    }

    // ── ANALYTICS ─────────────────────────────────────────────
    public Long getClickCount(String shortCode) {
        return repository.findByShortCode(shortCode)
                .map(UrlMapping::getClickCount)
                .orElseThrow(() -> new RuntimeException("Not found"));
    }

    // ── HELPERS ───────────────────────────────────────────────
    private String generateUniqueCode(String originalUrl) {
        String code = hashService.generateShortCode(originalUrl);
        // Collision check — retry if code exists
        int attempts = 0;
        while (repository.existsByShortCode(code) && attempts < 5) {
            code = hashService.generateShortCode(originalUrl + attempts);
            attempts++;
        }
        return code;
    }

    private void cacheUrl(String shortCode, String originalUrl) {
        redisTemplate.opsForValue().set(
                REDIS_PREFIX + shortCode,
                originalUrl,
                Duration.ofHours(CACHE_TTL_HOURS)  // TTL-based cache expiry
        );
    }

    @Async  // runs in background thread — doesn't delay the redirect
    public void incrementClickCountAsync(String shortCode) {
        repository.findByShortCode(shortCode).ifPresent(m -> {
            m.setClickCount(m.getClickCount() + 1);
            repository.save(m);
        });
    }
}
