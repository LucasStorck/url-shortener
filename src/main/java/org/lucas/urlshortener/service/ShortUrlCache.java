package org.lucas.urlshortener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class ShortUrlCache {

  private static final String KEY_PREFIX = "short-url:";
  private static final Duration MAX_TTL = Duration.ofHours(1);
  private static final Logger log = LoggerFactory.getLogger(ShortUrlCache.class);

  private final StringRedisTemplate redisTemplate;

  public ShortUrlCache(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public Optional<String> get(String code) {
    try {
      return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + code));
    } catch (RuntimeException e) {
      log.warn("Redis unavailable on get, falling back to the database: {}", e.getMessage());
      return Optional.empty();
    }
  }

  public void put(String code, String targetUrl, Instant expiresAt) {
    Duration ttl = MAX_TTL;
    if (expiresAt != null) {
      Duration untilExpiry = Duration.between(Instant.now(), expiresAt);
      if (untilExpiry.isNegative() || untilExpiry.isZero()) {
        return;
      }
      if (untilExpiry.compareTo(ttl) < 0) {
        ttl = untilExpiry;
      }
    }
    try {
      redisTemplate.opsForValue().set(KEY_PREFIX + code, targetUrl, ttl);
    } catch (RuntimeException e) {
      log.warn("Redis unavailable on put, skipping cache: {}", e.getMessage());
    }
  }
}
