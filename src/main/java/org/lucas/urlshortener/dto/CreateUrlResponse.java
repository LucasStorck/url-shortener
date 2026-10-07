package org.lucas.urlshortener.dto;

import java.time.Instant;

public record CreateUrlResponse(String code, String shortUrl, String targetUrl, Instant expiresAt) {
}
