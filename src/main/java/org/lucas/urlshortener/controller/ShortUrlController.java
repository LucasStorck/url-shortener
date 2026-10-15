package org.lucas.urlshortener.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.lucas.urlshortener.dto.CreateUrlRequest;
import org.lucas.urlshortener.dto.CreateUrlResponse;
import org.lucas.urlshortener.model.ShortUrl;
import org.lucas.urlshortener.service.ShortUrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
public class ShortUrlController {

  private final ShortUrlService service;

  public ShortUrlController(ShortUrlService service) {
    this.service = service;
  }

  @Operation(summary = "Create a short URL")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Short URL created"),
    @ApiResponse(responseCode = "400", description = "Invalid url (must be http/https, up to 2048 chars) or expiresInDays (1..3650). The body is a ProblemDetail with an 'errors' map of field to message",
      content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PostMapping("/api/urls")
  public ResponseEntity<CreateUrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
    ShortUrl created = service.create(request.url(), request.expiresInDays());
    URI shortUri = ServletUriComponentsBuilder.fromCurrentContextPath()
      .path("/{code}").buildAndExpand(created.getCode()).toUri();
    return ResponseEntity.created(shortUri).body(
      new CreateUrlResponse(created.getCode(), shortUri.toString(),
        created.getTargetUrl(), created.getExpiresAt())
    );
  }

  @Operation(summary = "Redirect to the original URL", description = "Responds 302 with the Location header. Swagger UI follows the redirect automatically; use curl -i to see the header.")
  @ApiResponses({
    @ApiResponse(responseCode = "302", description = "Redirect to the original URL", content = @Content),
    @ApiResponse(responseCode = "404", description = "Code not found", content = @Content),
    @ApiResponse(responseCode = "410", description = "Short URL expired", content = @Content)
  })
  @GetMapping("/{code}")
  public ResponseEntity<Void> redirect(@PathVariable String code) {
    String target = service.resolve(code);
    return ResponseEntity.status(HttpStatus.FOUND)
      .location(URI.create(target))
      .build();
  }
}
