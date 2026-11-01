package org.lucas.urlshortener.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lucas.urlshortener.repository.ShortUrlRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HitCounterTest {

  @Mock
  StringRedisTemplate redis;
  @Mock
  HashOperations<String, Object, Object> hashOps;
  @Mock
  ShortUrlRepository repository;

  MeterRegistry registry = new SimpleMeterRegistry();
  HitCounter hitCounter;

  @BeforeEach
  void setUp() {
    lenient().doReturn(hashOps).when(redis).opsForHash();
    hitCounter = new HitCounter(redis, repository, registry);
  }

  @Test
  void record_incrementsPendingHash() {
    assertThat(hitCounter.record("abc")).isTrue();

    verify(hashOps).increment("hits:pending", "abc", 1);
  }

  @Test
  void record_returnsFalseWhenRedisFails() {
    when(hashOps.increment("hits:pending", "abc", 1)).thenThrow(new RuntimeException("connection refused"));

    assertThat(hitCounter.record("abc")).isFalse();
  }

  @Test
  void flush_doesNothingWhenNoPendingHits() {
    when(redis.hasKey("hits:flushing")).thenReturn(false);
    when(redis.hasKey("hits:pending")).thenReturn(false);

    hitCounter.flush();

    verify(redis, never()).rename(any(), any());
    verify(repository, never()).addHits(any(), anyLong(), any());
  }

  @Test
  void flush_renamesPendingAndAppliesDeltas() {
    when(redis.hasKey("hits:flushing")).thenReturn(false);
    when(redis.hasKey("hits:pending")).thenReturn(true);
    when(hashOps.entries("hits:flushing")).thenReturn(Map.of("abc", "3"));

    hitCounter.flush();

    verify(redis).rename("hits:pending", "hits:flushing");
    verify(repository).addHits(eq("abc"), eq(3L), any(Instant.class));
    verify(hashOps).delete("hits:flushing", "abc");
    assertThat(registry.counter("urls.hits.flushed").count()).isEqualTo(3.0);
  }

  @Test
  void flush_resumesLeftoverBatchWithoutRenaming() {
    when(redis.hasKey("hits:flushing")).thenReturn(true);
    when(hashOps.entries("hits:flushing")).thenReturn(Map.of("abc", "2"));

    hitCounter.flush();

    verify(redis, never()).rename(any(), any());
    verify(repository).addHits(eq("abc"), eq(2L), any(Instant.class));
  }

  @Test
  void flush_keepsEntryWhenDatabaseFails() {
    when(redis.hasKey("hits:flushing")).thenReturn(true);
    when(hashOps.entries("hits:flushing")).thenReturn(Map.of("abc", "2"));
    when(repository.addHits(eq("abc"), eq(2L), any(Instant.class))).thenThrow(new RuntimeException("db down"));

    hitCounter.flush();

    verify(hashOps, never()).delete(any(), any());
    assertThat(registry.counter("urls.hits.flush.errors").count()).isEqualTo(1.0);
  }
}
