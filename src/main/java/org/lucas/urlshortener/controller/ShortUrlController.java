package org.lucas.urlshortener.controller;

import jakarta.validation.Valid;
import org.lucas.urlshortener.dto.CreateUrlRequest;
import org.lucas.urlshortener.dto.CreateUrlResponse;
import org.lucas.urlshortener.model.ShortUrl;
import org.lucas.urlshortener.service.ShortUrlService;
import org.springframework.http.HttpStatus;
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

  @GetMapping("/{code}")
  public ResponseEntity<Void> redirect(@PathVariable String code) {
    String target = service.resolve(code);
    return ResponseEntity.status(HttpStatus.FOUND)
      .location(URI.create(target))
      .build();
  }
}
