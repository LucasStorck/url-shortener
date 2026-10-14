package org.lucas.urlshortener.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;

public class HttpUrlValidator implements ConstraintValidator<ValidHttpUrl, String> {
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null)
      return true;
    try {
      URI uri = URI.create(value);
      String scheme = uri.getScheme();
      return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
        && uri.getHost() != null;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
