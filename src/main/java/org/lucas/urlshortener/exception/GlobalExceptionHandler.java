package org.lucas.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ShortUrlNotFoundException.class)
  public ResponseEntity<ProblemDetail> notFound(ShortUrlNotFoundException e) {
    return ResponseEntity.status(404).body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage()));
  }

  @ExceptionHandler(ShortUrlExpiredException.class)
  public ResponseEntity<ProblemDetail> expired(ShortUrlExpiredException e) {
    return ResponseEntity.status(410).body(ProblemDetail.forStatusAndDetail(HttpStatus.GONE, e.getMessage()));
  }

  @ExceptionHandler(ShortUrlCodeGenerationException.class)
  public ResponseEntity<ProblemDetail> generationException(ShortUrlCodeGenerationException e){
    return ResponseEntity.status(500).body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));

  }
}
