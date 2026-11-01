package org.lucas.urlshortener.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lucas.urlshortener.exception.ShortUrlCodeGenerationException;
import org.lucas.urlshortener.exception.ShortUrlExpiredException;
import org.lucas.urlshortener.exception.ShortUrlNotFoundException;
import org.lucas.urlshortener.model.ShortUrl;
import org.lucas.urlshortener.repository.ShortUrlRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ShortUrlServiceImplTest {

  @Mock
  ShortUrlRepository repository;
  @InjectMocks
  ShortUrlServiceImpl service;
  @Mock
  ShortUrlCache cache;
  @Mock
  HitCounter hitCounter;

  @Spy
  private MeterRegistry registry = new SimpleMeterRegistry();

  @Test
  public void create_savesEntityWithGeneratedCode() {
    when(repository.saveAndFlush(any(ShortUrl.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));

    ShortUrl result = service.create("https://example.com", null);
    assertThat(result.getCode()).hasSize(7);
    assertThat(result.getTargetUrl()).isEqualTo("https://example.com");
    assertThat(result.getExpiresAt()).isNull();
    assertThat(registry.counter("urls.shortened").count()).isEqualTo(1.0);
  }

  @Test
  public void create_withExpiresInDays_setsExpiresAtInTheFuture() {
    when(repository.saveAndFlush(any(ShortUrl.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    ShortUrl result = service.create("https://example.com", 2);

    assertThat(result.getExpiresAt())
      .isCloseTo(Instant.now().plus(2, ChronoUnit.DAYS), org.assertj.core.api.Assertions.within(5, ChronoUnit.SECONDS));
  }

  @Test
  public void create_failsAfterMaxAttempts() {
    when(repository.saveAndFlush(any(ShortUrl.class)))
      .thenThrow(new DataIntegrityViolationException("duplicate"));

    assertThatThrownBy(() -> service.create("https://example.com", null))
      .isInstanceOf(ShortUrlCodeGenerationException.class);

    verify(repository, times(5)).saveAndFlush(any(ShortUrl.class));
  }

  @Test
  void create_retriesOnCollisionAndSucceeds() {
    when(repository.saveAndFlush(any(ShortUrl.class)))
      .thenThrow(new DataIntegrityViolationException("duplicate"))
      .thenAnswer(invocation -> invocation.getArgument(0));

    ShortUrl result = service.create("https://example.com", null);

    assertThat(result).isNotNull();
    assertThat(result.getCode()).hasSize(7);
    assertThat(registry.counter("urls.code.collisions").count()).isEqualTo(1.0);
    verify(repository, times(2)).saveAndFlush(any(ShortUrl.class));
  }

  @Test
  public void resolve_returnsTargetAndRegistersHit() {
    ShortUrl entity = new ShortUrl();
    entity.setId("id-1");
    entity.setCode("abc1234");
    entity.setTargetUrl("https://example.com");
    when(repository.findByCode("abc1234")).thenReturn(Optional.of(entity));
    when(hitCounter.record("abc1234")).thenReturn(true);
    String target = service.resolve("abc1234");

    assertThat(target).isEqualTo("https://example.com");
    assertThat(redirects("miss")).isEqualTo(1.0);
    verify(hitCounter).record("abc1234");
    verify(repository, never()).registerHit(any(), any());
  }

  @Test
  public void resolve_throwsNotFound() {
    when(repository.findByCode("nope")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.resolve("nope"))
      .isInstanceOf(ShortUrlNotFoundException.class);

    assertThat(redirects("not_found")).isEqualTo(1.0);
    verify(hitCounter, never()).record(any());
    verify(repository, never()).registerHit(any(), any());
  }

  @Test
  public void resolve_throwsExpiredAndDoesNotRegisterHit() {
    ShortUrl entity = new ShortUrl();
    entity.setId("id-1");
    entity.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
    when(repository.findByCode("old")).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> service.resolve("old"))
      .isInstanceOf(ShortUrlExpiredException.class);

    assertThat(redirects("expired")).isEqualTo(1.0);
    verify(hitCounter, never()).record(any());
    verify(repository, never()).registerHit(any(), any());
  }

  @Test
  void resolve_cacheHit_skipsDatabaseAndCountsInRedis() {
    when(cache.get("abc1234")).thenReturn(Optional.of("https://example.com"));

    when(hitCounter.record("abc1234")).thenReturn(true);
    String target = service.resolve("abc1234");

    assertThat(target).isEqualTo("https://example.com");
    assertThat(redirects("hit")).isEqualTo(1.0);
    verify(repository, never()).findByCode(any());
    verify(hitCounter).record("abc1234");
    verify(repository, never()).registerHit(any(), any());
  }

  @Test
  void resolve_cacheMiss_populatesCache() {
    ShortUrl entity = new ShortUrl();
    entity.setCode("abc1234");
    entity.setTargetUrl("https://example.com");
    when(repository.findByCode("abc1234")).thenReturn(Optional.of(entity));
    when(hitCounter.record("abc1234")).thenReturn(true);

    service.resolve("abc1234");

    verify(cache).put("abc1234", "https://example.com", null);
    verify(hitCounter).record("abc1234");
  }

  @Test
  void resolve_expired_doesNotPopulateCache() {
    ShortUrl entity = new ShortUrl();
    entity.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
    when(repository.findByCode("old")).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> service.resolve("old"))
      .isInstanceOf(ShortUrlExpiredException.class);

    verify(cache, never()).put(any(), any(), any());
  }

  @Test
  void resolve_fallsBackToSynchronousCountWhenRedisFails() {
    ShortUrl entity = new ShortUrl();
    entity.setCode("abc1234");
    entity.setTargetUrl("https://example.com");
    when(repository.findByCode("abc1234")).thenReturn(Optional.of(entity));
    when(hitCounter.record("abc1234")).thenReturn(false);

    service.resolve("abc1234");

    verify(repository).registerHit(eq("abc1234"), any(Instant.class));
    assertThat(registry.counter("urls.hits.fallback").count()).isEqualTo(1.0);
  }

  private double redirects(String result) {
    return registry.counter("urls.redirects", "result", result).count();
  }

}
