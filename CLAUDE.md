# URL Shortener

Encurtador de URLs com métricas, feito com Java e Spring Boot. Projeto de aprendizado/portfólio.
O dono quer **entender** o código: explique decisões e trade-offs, e prefira mostrar o código para ele implementar manualmente quando pedir.

## Especificação base
- Redis para cache e rate limiting (Bucket4j)
- Contagem de cliques com processamento assíncrono
- Spring Actuator + Micrometer + Prometheus/Grafana para observabilidade

## Stack
- Java 25, Spring Boot 4.1.1, Maven (wrapper: `./mvnw`)
- PostgreSQL 15 + Liquibase (schema), Redis 7 (ainda sem uso)
- `spring-boot-docker-compose`: ao subir a app, o `compose.yaml` sobe o Postgres e o Redis e a conexão é configurada automaticamente (por isso as portas são publicadas sem porta fixa no host)
- Já no `pom.xml`: Actuator, Micrometer Prometheus, datasource-micrometer, validation

## Comandos
```bash
./mvnw compile                # compila
./mvnw spring-boot:run        # sobe a app (e o docker compose junto)
```

## Convenções
- Pacotes: `model`, `repository`, `dto`, `service` (interface + `Impl`), `controller`, `exception`
- DTOs são records com bean validation; nunca expor entidades na API
- Injeção por construtor
- Recurso inexistente lança exceção própria, mapeada no `GlobalExceptionHandler` (`@RestControllerAdvice`)
- Indentação de 2 espaços
- Schema só via Liquibase (`db/changelog/`); `ddl-auto: validate`
- Commits: Conventional Commits, em inglês (`feat: ...`, `build: ...`)
- Git: hoje direto no `main`; preferível um branch por feature + PR

## O que já foi feito
- Projeto Spring Boot, `compose.yaml` (Postgres + Redis com healthchecks), repositório no GitHub
- `ShortUrl` (entidade) e `ShortUrlRepository` com `findByCode`, `existsByCode` e `registerHit` (incremento atômico de `hits`)
- Changelog Liquibase `001-create-short-urls` (tabela `short_urls`, `code` único)
- `Base62.randomCode(length)`: sorteia cada caractere com `SecureRandom`
- DTOs `CreateUrlRequest` e `CreateUrlResponse`
- `ShortUrlService`/`ShortUrlServiceImpl`: `create` (tenta até 5 códigos em caso de colisão no índice único) e `resolve`
- `ShortUrlController`: `POST /api/urls` (201 + `Location`) e `GET /{code}` (302)
- Exceções (`ShortUrlNotFoundException` 404, `ShortUrlExpiredException` 410, `ShortUrlCodeGenerationException` 500) e `GlobalExceptionHandler`
- Compila (`./mvnw compile`), mas **a app ainda não foi executada nem testada**

## Erros a corrigir
1. **`ShortUrlServiceImpl.create`**: `entity.setTargetUrl(Base62.randomCode(...))` deveria ser `entity.setCode(...)`. Hoje o `code` fica nulo, o insert falha e o `POST /api/urls` sempre devolve 500
2. **`catch` vazio** de `DataIntegrityViolationException` no `create`: esconde a causa real (qualquer violação vira "colisão"). Logar com `warn` e formatar o `} try {`
3. **`CreateUrlRequest`**: `@URL` aceita `ftp://` e `file://` (usar `@Pattern(regexp = "^https?://.+")`) e `expiresInDays` sem `@Max` estoura o `Instant` com 500 (ex.: `@Max(3650)`)
4. **`ShortUrlRepository`**: usa `jakarta.transaction.Transactional`; trocar por `org.springframework.transaction.annotation.Transactional`
5. Mensagens de exceção inconsistentes ("Short URL Expired", "COULD NOT GENERATE A UNIQUE CODE"); padronizar caixa
6. `existsByCode` não é usado (checar antes de salvar tem race condition; o índice único é quem decide) e pode sair
7. `UrlShortenerApplicationTests.contextLoads` vai falhar sem banco (o docker compose é desligado em testes); resolver com Testcontainers

## Próximos passos
1. Corrigir os erros acima, subir a app e testar com `curl` (201, 302, 404, 410, 400)
2. **Cache no Redis** do `code → targetUrl` no `resolve` (TTL respeitando `expiresAt`)
3. **Contagem de cliques assíncrona**: hoje `registerHit` é síncrono no caminho do redirect, o que contradiz a especificação. Opções: `@Async`, ou contar no Redis (`INCR`) e sincronizar com o Postgres em lote por job agendado
4. **Rate limiting com Bucket4j** apoiado no Redis (por IP), com resposta 429
5. **Observabilidade**: expor `/actuator/prometheus`, métricas customizadas com Micrometer (criações, redirects, cache hit/miss, falhas), e adicionar Prometheus e Grafana ao `compose.yaml` com um dashboard
6. Testes: unitários do service, de integração do controller (Testcontainers)
7. Opcional: endpoint de estatísticas por código, limpeza de URLs expiradas
