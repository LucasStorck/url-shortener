package org.lucas.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlCacheTest {

  @Mock
  StringRedisTemplate redis;
  @Mock
  ValueOperations<String, String> ops;

  ShortUrlCache cache;

  @BeforeEach
  void setUp() {
    lenient().when(redis.opsForValue()).thenReturn(ops);
    cache = new ShortUrlCache(redis);
  }

  @Test
  void get_returnsEmptyWhenRedisFails() {
    when(ops.get("short-url:abc")).thenThrow(new RuntimeException("connection refused"));

    assertThat(cache.get("abc")).isEmpty();
  }

  @Test
  void put_usesMaxTtlWhenNoExpiry() {
    cache.put("abc", "https://example.com", null);

    verify(ops).set("short-url:abc", "https://example.com", Duration.ofHours(1));
  }

  @Test
  void put_usesTimeUntilExpiryWhenShorter() {
    cache.put("abc", "https://example.com", Instant.now().plus(10, ChronoUnit.MINUTES));

    ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
    verify(ops).set(eq("short-url:abc"), eq("https://example.com"), ttl.capture());
    assertThat(ttl.getValue()).isLessThanOrEqualTo(Duration.ofMinutes(10));
  }

  @Test
  void put_skipsWhenAlreadyExpired() {
    cache.put("abc", "https://example.com", Instant.now().minusSeconds(5));

    verify(ops, never()).set(any(), any(), any(Duration.class));
  }
}
