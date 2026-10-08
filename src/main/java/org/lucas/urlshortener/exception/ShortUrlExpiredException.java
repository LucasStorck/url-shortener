package org.lucas.urlshortener.exception;

public class ShortUrlExpiredException extends RuntimeException {
  public ShortUrlExpiredException(String code){
    super("Short URL Expired: " + code);
  }
}
