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

@Service
public class ShortUrlServiceImpl implements ShortUrlService {

  private static final int CODE_LENGTH = 7;
  private static final int MAX_ATTEMPTS = 5;
  private static final Logger log = LoggerFactory.getLogger(ShortUrlServiceImpl.class);

  private final ShortUrlRepository repository;

  public ShortUrlServiceImpl(ShortUrlRepository repository) {
    this.repository = repository;
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
        log.warn("Code Collision Generating Short URL, Retrying (Attempts {}/{})", attempt, MAX_ATTEMPTS, e);
      }
    }
    throw new ShortUrlCodeGenerationException(MAX_ATTEMPTS);
  }

  @Override
  public String resolve(String code) {
    ShortUrl entity = repository.findByCode(code)
            .orElseThrow(() -> new ShortUrlNotFoundException(code));

    if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(Instant.now()))
      throw new ShortUrlExpiredException(code);
    repository.registerHit(entity.getId(), Instant.now());
    return entity.getTargetUrl();
  }
}
