package org.lucas.urlshortener.exception;

public class ShortUrlNotFoundException extends RuntimeException {
  public ShortUrlNotFoundException(String code) {
    super("Short URL Not Found: " + code);
  }
}
