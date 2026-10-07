package org.lucas.urlshortener.repository;

import jakarta.transaction.Transactional;
import org.lucas.urlshortener.model.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, String> {

  Optional<ShortUrl> findByCode(String code);

  boolean existsByCode(String code);

  @Modifying
  @Transactional
  @Query("update ShortUrl s set s.hits = s.hits + 1, s.lastAccessedAt = :now where s.id = :id")
  void registerHit(@Param("id") String id, @Param("now") Instant now);
}
