package org.lucas.urlshortener.exception;

public class ShortUrlCodeGenerationException extends RuntimeException{
  public ShortUrlCodeGenerationException(String message){
    super(message);
  }
}
