package org.lucas.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CreateUrlRequest(@NotBlank @Size(max = 2048) @URL String url, @Positive Integer expiresInDays) {
}
