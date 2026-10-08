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
- Já no `pom.xml`: Actuator, Micrometer Prometheus, datasource-micrometer, validation, `spring-boot-starter-liquibase`, Testcontainers (`spring-boot-testcontainers` + `junit-jupiter` + `postgresql`, BOM em `1.21.4`)
- **Atenção (Spring Boot 4.x)**: a autoconfiguração do Liquibase foi modularizada — `org.liquibase:liquibase-core` sozinho só traz a ferramenta de migração, não o bean `SpringLiquibase`. É preciso o starter `spring-boot-starter-liquibase` para o Spring de fato rodar as migrations
- Testcontainers `1.20.x` não negocia corretamente a API do Docker com engines mais novos (erro "client version 1.32 is too old"); por isso o BOM está fixado em `1.21.4`

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
- `ShortUrl` (entidade) e `ShortUrlRepository` com `findByCode` e `registerHit` (incremento atômico de `hits`)
- Changelog Liquibase `001-create-short-urls` (tabela `short_urls`, `code` único)
- `Base62.randomCode(length)`: sorteia cada caractere com `SecureRandom`
- DTOs `CreateUrlRequest` e `CreateUrlResponse`
- `ShortUrlService`/`ShortUrlServiceImpl`: `create` (tenta até 5 códigos em caso de colisão no índice único) e `resolve`
- `ShortUrlController`: `POST /api/urls` (201 + `Location`) e `GET /{code}` (302)
- Exceções (`ShortUrlNotFoundException` 404, `ShortUrlExpiredException` 410, `ShortUrlCodeGenerationException` 500) e `GlobalExceptionHandler`
- Os 7 erros que existiam em `create`/validação/repositório/exceções/teste de contexto foram corrigidos (bug do `setCode`, catch vazio, validação do `@URL`/`expiresInDays`, `Transactional` errado, mensagens inconsistentes, `existsByCode` morto, `contextLoads` sem banco)
- `UrlShortenerApplicationTests.contextLoads` usa Testcontainers (`@ServiceConnection` + `@Container` com `PostgreSQLContainer`); Liquibase roda as migrations contra o container e o Hibernate valida o schema
- `./mvnw test` passa (contexto completo sobe com sucesso), mas **a app ainda não foi executada com `spring-boot:run` nem testada manualmente com `curl`**

## Próximos passos
1. Subir a app (`spring-boot:run`) e testar os endpoints com `curl` (201, 302, 404, 410, 400)
2. **Cache no Redis** do `code → targetUrl` no `resolve` (TTL respeitando `expiresAt`)
3. **Contagem de cliques assíncrona**: hoje `registerHit` é síncrono no caminho do redirect, o que contradiz a especificação. Opções: `@Async`, ou contar no Redis (`INCR`) e sincronizar com o Postgres em lote por job agendado
4. **Rate limiting com Bucket4j** apoiado no Redis (por IP), com resposta 429
5. **Observabilidade**: expor `/actuator/prometheus`, métricas customizadas com Micrometer (criações, redirects, cache hit/miss, falhas), e adicionar Prometheus e Grafana ao `compose.yaml` com um dashboard
6. Testes: unitários do service, de integração do controller (Testcontainers)
7. Opcional: endpoint de estatísticas por código, limpeza de URLs expiradas
