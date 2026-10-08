package org.lucas.urlshortener.dto;

import jakarta.validation.constraints.*;

public record CreateUrlRequest(@NotBlank @Size(max = 2048) @Pattern(regexp = "^https?://.+") String url, @Positive @Max(3650) Integer expiresInDays) {
}
