package org.lucas.urlshortener.service;

import org.lucas.urlshortener.model.ShortUrl;

public interface ShortUrlService {
  ShortUrl create(String targetUrl, Integer expiresInDays);
  String resolve(String code);
}
