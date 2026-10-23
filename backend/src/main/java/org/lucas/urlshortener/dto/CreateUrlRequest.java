package org.lucas.urlshortener.dto;

import jakarta.validation.constraints.*;

public record CreateUrlRequest(
  @NotBlank(message = "must not be blank") @Size(max = 2048, message = "must be at most 2048 characters")
  @ValidHttpUrl
  String url,
  @Positive(message = "must be greater than 0")
  @Max(value = 3650, message = "must be at most 3650 days")
  Integer expiresInDays) {
}
