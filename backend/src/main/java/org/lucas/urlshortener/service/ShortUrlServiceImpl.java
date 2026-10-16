package org.lucas.urlshortener.service;

import org.lucas.urlshortener.exception.ShortUrlCodeGenerationException;
import org.lucas.urlshortener.exception.ShortUrlExpiredException;
import org.lucas.urlshortener.exception.ShortUrlNotFoundException;
import org.lucas.urlshortener.model.Base62;
import org.lucas.urlshortener.model.ShortUrl;
import org.lucas.urlshortener.repository.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class ShortUrlServiceImpl implements ShortUrlService {

  private static final int CODE_LENGTH = 7;
  private static final int MAX_ATTEMPTS = 5;
  private static final Logger log = LoggerFactory.getLogger(ShortUrlServiceImpl.class);

  private final ShortUrlRepository repository;
  private final ShortUrlCache cache;

  public ShortUrlServiceImpl(ShortUrlRepository repository, ShortUrlCache cache) {
    this.repository = repository;
    this.cache = cache;
  }

  @Override
  public ShortUrl create(String targetUrl, Integer expiresInDays) {
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      ShortUrl entity = new ShortUrl();
      entity.setCode(Base62.randomCode(CODE_LENGTH));
      entity.setTargetUrl(targetUrl);
      if (expiresInDays != null) {
        entity.setExpiresAt(Instant.now().plus(expiresInDays, ChronoUnit.DAYS));
      }
      try {
        return repository.saveAndFlush(entity);
      } catch (DataIntegrityViolationException e) {
        log.warn("Code collision generating short URL, retrying (attempts {}/{})", attempt + 1, MAX_ATTEMPTS);
      }
    }
    throw new ShortUrlCodeGenerationException(MAX_ATTEMPTS);
  }

  @Override
  public String resolve(String code) {
    Optional<String> cached = cache.get(code);
    if (cached.isPresent()) {
      repository.registerHit(code, Instant.now());
      return cached.get();
    }

    ShortUrl entity = repository.findByCode(code)
      .orElseThrow(() -> new ShortUrlNotFoundException(code));

    if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(Instant.now())) {
      throw new ShortUrlExpiredException(code);
    }

    cache.put(code, entity.getTargetUrl(), entity.getExpiresAt());
    repository.registerHit(code, Instant.now());
    return entity.getTargetUrl();
  }
}
