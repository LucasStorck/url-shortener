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
- Swagger UI via `springdoc-openapi-starter-webmvc-ui` `3.1.1` (linha do Spring Boot 4; versão fixada no `pom.xml`, fora do BOM do Boot)
- Spring Boot 4 usa **Jackson 3**: o pacote é `tools.jackson.databind` (`ObjectMapper`, `JsonNode`), não `com.fasterxml.jackson.databind`. Em testes, `@AutoConfigureMockMvc` vem de `org.springframework.boot.webmvc.test.autoconfigure`

## Comandos
```bash
./mvnw compile                # compila
./mvnw test                   # testes (precisa do Docker rodando, por causa do Testcontainers)
./mvnw spring-boot:run        # sobe a app (e o docker compose junto)
```
- Swagger UI: `http://localhost:8080/swagger-ui.html`; especificação em `/v3/api-docs`
- O Swagger UI usa `fetch` e segue o redirect do `GET /{code}` sozinho: contra o GitHub dá "Failed to fetch" (CORS), mas o hit é contabilizado. Para ver o 302 e o `Location`, usar `curl -i` ou o navegador
- No campo `code` do Swagger colar **só o código**, nunca o link curto inteiro (a `/` codificada como `%2F` faz o Tomcat devolver um 400 em HTML, fora do `GlobalExceptionHandler`)

## Convenções
- Pacotes: `model`, `repository`, `dto`, `service` (interface + `Impl`), `controller`, `exception`
- DTOs são records com bean validation; nunca expor entidades na API
- Injeção por construtor
- Recurso inexistente lança exceção própria, mapeada no `GlobalExceptionHandler` (`@RestControllerAdvice`)
- Indentação de 2 espaços
- Schema só via Liquibase (`db/changelog/`); `ddl-auto: validate`
- Commits: Conventional Commits, em inglês (`feat: ...`, `build: ...`)
- Git: um branch por feature/correção (`feat/...`, `fix/...`) + PR para o `main`; antes de começar, `git pull` no `main` (já houve trabalho feito em outra máquina que não estava no disco local)
- Commits sem linha `Co-Authored-By` (pedido do dono)

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
- `./mvnw test` passa (contexto completo sobe com sucesso)
- Swagger UI (branch `feat/swagger`): `@Operation`/`@ApiResponses` em `ShortUrlController` documentando 201/400 (`POST`) e 302/404/410 (`GET`); a especificação já reflete as regras de validação do DTO
- **Validado no fluxo real** (app rodando pelo IntelliJ + Swagger + navegador): `POST /api/urls` devolve 201 com `code` de 7 caracteres, `shortUrl` montado com o host da requisição e `expiresAt` correto; abrir o link curto leva ao destino (302); `hits` é incrementado a cada acesso que chega à API, inclusive o do Swagger que falha por CORS
- Também validado com testes `MockMvc` + Testcontainers (descartados, não estão no repositório): 404, 410 (e `hits` não sobe), 400 para `ftp://`, `file://`, `javascript:`, URL vazia, body `{}`, `expiresInDays` 0/negativo/2000000000 e URL > 2048 caracteres, e 200 criações com 200 códigos distintos

## Problemas conhecidos
1. **URL com espaço** (`https://example.com/a b`): passa no `@Pattern(regexp = "^https?://.+")`, o `POST` devolve 201, mas o `GET /{code}` lança `IllegalArgumentException` em `URI.create(target)` no controller e devolve 500; o link fica quebrado. Corrigir validando com `URI.create` no momento da criação (400) ou rejeitando espaços no regex
2. Log de colisão em `create` mostra `attempt` começando em 0 ("0/5"); usar `attempt + 1`
3. Mensagens das exceções em Title Case ("Short URL Not Found", "Could Not Generate A Unique Code After N Attempts"); padronizar em frase normal
4. `CreateUrlRequest` usa `import jakarta.validation.constraints.*`; preferir imports explícitos
5. **Máquina do trabalho**: `./mvnw spring-boot:run` iniciado pelo Claude falha com `Unable to establish loopback connection` ao subir o Tomcat (JVM/Windows, provável VPN/antivírus/EDR); não é problema do código. Pelo IntelliJ a app sobe normalmente. Testes com `MockMvc` não abrem porta e funcionam

## Próximos passos
1. Corrigir o problema conhecido nº 1 (URL com espaço) num branch `fix/validate-url`, e os itens 2 a 4
2. **Cache no Redis** do `code → targetUrl` no `resolve` (TTL respeitando `expiresAt`)
3. **Contagem de cliques assíncrona**: hoje `registerHit` é síncrono no caminho do redirect, o que contradiz a especificação. Opções: `@Async`, ou contar no Redis (`INCR`) e sincronizar com o Postgres em lote por job agendado
4. **Rate limiting com Bucket4j** apoiado no Redis (por IP), com resposta 429
5. **Observabilidade**: expor `/actuator/prometheus`, métricas customizadas com Micrometer (criações, redirects, cache hit/miss, falhas), e adicionar Prometheus e Grafana ao `compose.yaml` com um dashboard
6. Testes: unitários do service, de integração do controller (`MockMvc` + Testcontainers, cobrindo os cenários já validados à mão). Na contagem de cliques, considerar filtrar robôs e pré-visualizadores de link (WhatsApp, Slack) por `User-Agent`
7. Opcional: endpoint de estatísticas por código, limpeza de URLs expiradas
