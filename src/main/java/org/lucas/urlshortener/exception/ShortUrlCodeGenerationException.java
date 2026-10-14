package org.lucas.urlshortener.exception;

public class ShortUrlCodeGenerationException extends RuntimeException{
  public ShortUrlCodeGenerationException(Integer attempts){
    super("Could not generate a unique code after " + attempts + " attempts");
  }
}
