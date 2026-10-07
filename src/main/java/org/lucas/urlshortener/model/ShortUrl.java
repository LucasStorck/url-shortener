package org.lucas.urlshortener.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "short_urls")
public class ShortUrl {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false, unique = true, length = 32)
  private String code;

  @Column(name = "target_url", nullable = false, length = 2048)
  private String targetUrl;

  @Column(name = "expires_at")
  private Instant expiresAt;

  private long hits = 0;

  @Column(name = "last_accessed_at")
  private Instant lastAccessedAt;

  @Column(name = "created_at")
  private Instant createdAt = Instant.now();

  public ShortUrl(){}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getTargetUrl() {
    return targetUrl;
  }

  public void setTargetUrl(String targetUrl) {
    this.targetUrl = targetUrl;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public long getHits() {
    return hits;
  }

  public void setHits(long hits) {
    this.hits = hits;
  }

  public Instant getLastAccessedAt() {
    return lastAccessedAt;
  }

  public void setLastAccessedAt(Instant lastAccessedAt) {
    this.lastAccessedAt = lastAccessedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
