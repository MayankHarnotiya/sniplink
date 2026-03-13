package com.snip.link.controller;

import com.snip.link.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService service;

    @Value("${APP_BASE_URL:http://localhost:8080/}")
    private String baseUrl;

    // POST /shorten  →  creates a short URL
    @PostMapping("/shorten")
    public ResponseEntity<Map<String, String>> shorten(@RequestBody Map<String, String> body) {
        String originalUrl = body.get("url");
        String shortCode = service.shortenUrl(originalUrl);

        return ResponseEntity.ok(Map.of(
                "shortUrl", baseUrl + shortCode,
                "shortCode", shortCode
        ));
    }

    // GET /{code}  →  redirects to original URL
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = service.resolveUrl(shortCode);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }

    // GET /analytics/{code}  →  click count
    @GetMapping("/analytics/{shortCode}")
    public ResponseEntity<Map<String, Object>> analytics(@PathVariable String shortCode) {
        Long clicks = service.getClickCount(shortCode);
        return ResponseEntity.ok(Map.of("shortCode", shortCode, "clicks", clicks));
    }
}