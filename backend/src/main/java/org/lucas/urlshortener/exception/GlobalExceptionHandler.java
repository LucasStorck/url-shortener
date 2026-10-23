package org.lucas.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

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
  public ResponseEntity<ProblemDetail> generationException(ShortUrlCodeGenerationException e) {
    return ResponseEntity.status(500).body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));

  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e) {
    Map<String, String> errors = new LinkedHashMap<>();
    e.getBindingResult().getFieldErrors()
      .forEach(fieldError -> errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request");
    problemDetail.setProperty("errors", errors);
    return ResponseEntity.badRequest().body(problemDetail);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> notReadable(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest()
      .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request"));
  }
}
