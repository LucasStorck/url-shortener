package org.lucas.urlshortener.exception;

public class ShortUrlCodeGenerationException extends RuntimeException{
  public ShortUrlCodeGenerationException(Integer attempts){
    super("Could Not Generate A Unique Code After " + attempts + " Attempts");
  }
}
