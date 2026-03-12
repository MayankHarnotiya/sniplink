package com.snip.link.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "url_mappings")
@Data  // Lombok: generates getters, setters, toString
@NoArgsConstructor
@AllArgsConstructor
public class UrlMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String shortCode;      // e.g., "abc123"

    @Column(nullable = false, length = 2048)
    private String originalUrl;    // the long URL

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime expiresAt;  // for TTL-based expiry

    @Column(nullable = false)
    private Long clickCount = 0L;

}
