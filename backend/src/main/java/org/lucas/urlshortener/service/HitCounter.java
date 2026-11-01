package org.lucas.urlshortener.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.lucas.urlshortener.repository.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class HitCounter {

  private static final String PENDING_KEY = "hits:pending";
  private static final String FLUSHING_KEY = "hits:flushing";
  private static final Logger log = LoggerFactory.getLogger(HitCounter.class);

  private final StringRedisTemplate stringRedisTemplate;
  private final ShortUrlRepository shortUrlRepository;
  private final MeterRegistry meterRegistry;

  public HitCounter(StringRedisTemplate stringRedisTemplate, ShortUrlRepository shortUrlRepository, MeterRegistry meterRegistry) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.shortUrlRepository = shortUrlRepository;
    this.meterRegistry = meterRegistry;
  }

  public boolean record(String code) {
    try {
      stringRedisTemplate.opsForHash().increment(PENDING_KEY, code, 1);
      return true;
    } catch (RuntimeException e) {
      log.warn("Redis unavailable on hit count, falling back to the database: {}", e.getMessage());
      return false;
    }
  }

  @Scheduled(fixedDelayString = "${app.hits.flush-interval:PT10S}")
  public void flush() {
    try {
      if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(FLUSHING_KEY))) {
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(PENDING_KEY))) {
          return;
        }
        stringRedisTemplate.rename(PENDING_KEY, FLUSHING_KEY);
      }
      Map<Object, Object> batch = stringRedisTemplate.opsForHash().entries(FLUSHING_KEY);
      Instant now = Instant.now();
      for (Map.Entry<Object, Object> entry : batch.entrySet()) {
        String code = (String) entry.getKey();
        long delta = Long.parseLong((String) entry.getValue());
        shortUrlRepository.addHits(code, delta, now);
        stringRedisTemplate.opsForHash().delete(FLUSHING_KEY, code);
        meterRegistry.counter("urls.hits.flushed").increment(delta);
      }
    } catch (RuntimeException e) {
      meterRegistry.counter("urls.hits.flush.errors").increment();
      log.warn("Hit flush failed, will retry on the next run: {}", e.getMessage());
    }
  }
}
