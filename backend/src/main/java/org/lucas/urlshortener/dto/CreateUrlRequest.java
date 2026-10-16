package org.lucas.urlshortener.dto;

import jakarta.validation.constraints.*;

public record CreateUrlRequest(@NotBlank @Size(max = 2048) @ValidHttpUrl String url, @Positive @Max(3650) Integer expiresInDays) {
}
