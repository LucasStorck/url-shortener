package org.lucas.urlshortener.repository;

import org.lucas.urlshortener.model.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, String> {

  Optional<ShortUrl> findByCode(String code);

  boolean existsByCode(String code);
}
