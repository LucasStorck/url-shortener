# URL Shortener

Encurtador de URLs com métricas, feito com Java e Spring Boot. Projeto de aprendizado/portfólio.
O dono quer **entender** o código: explique decisões e trade-offs, e prefira mostrar o código para ele implementar manualmente quando pedir.

## Especificação base
- Redis para cache e rate limiting (Bucket4j)
- Contagem de cliques com processamento assíncrono
- Spring Actuator + Micrometer + Prometheus/Grafana para observabilidade

## Stack
- Java 25, Spring Boot 4.1.1, Maven (wrapper: `./mvnw`)
- PostgreSQL 15 + Liquibase (schema), Redis 7 (cache do `resolve`)
- `spring-boot-docker-compose`: ao subir a app, o `compose.yaml` sobe o Postgres e o Redis e a conexão é configurada automaticamente (por isso as portas são publicadas sem porta fixa no host)
- Já no `pom.xml`: Actuator, Micrometer Prometheus, datasource-micrometer, validation, `spring-boot-starter-liquibase`, Testcontainers (`spring-boot-testcontainers` + `junit-jupiter` + `postgresql`, BOM em `1.21.4`)
- **Atenção (Spring Boot 4.x)**: a autoconfiguração do Liquibase foi modularizada — `org.liquibase:liquibase-core` sozinho só traz a ferramenta de migração, não o bean `SpringLiquibase`. É preciso o starter `spring-boot-starter-liquibase` para o Spring de fato rodar as migrations
- Testcontainers `1.20.x` não negocia corretamente a API do Docker com engines mais novos (erro "client version 1.32 is too old"); por isso o BOM está fixado em `1.21.4`
- Swagger UI via `springdoc-openapi-starter-webmvc-ui` `3.1.1` (linha do Spring Boot 4; versão fixada no `pom.xml`, fora do BOM do Boot)
- Spring Boot 4 usa **Jackson 3**: o pacote é `tools.jackson.databind` (`ObjectMapper`, `JsonNode`), não `com.fasterxml.jackson.databind`. Em testes, `@AutoConfigureMockMvc` vem de `org.springframework.boot.webmvc.test.autoconfigure`
- Frontend (planejado, pasta `frontend/`): React + TypeScript com Vite, Tailwind CSS e componentes do [neobrutalism.dev](https://www.neobrutalism.dev/) (construído sobre shadcn/ui: os componentes são **copiados** para o projeto, em `src/components/ui/`, via CLI `npx shadcn@latest add <url do componente>`, e não instalados como pacote; podem ser editados à mão). Registrar aqui as versões ao criar o projeto (React, Vite, Tailwind)
- Dev do frontend: `npm run dev` (Vite) em `http://localhost:5173` com `server.proxy` no `vite.config.ts` apontando `/api` para `http://localhost:8080`, para não exigir CORS no backend. O `shortUrl` é montado com o host da requisição: pelo proxy ele vem com a porta 5173 e o `GET /{code}` também precisaria passar pelo proxy; abrir o link curto sempre pela porta 8080

## Comandos
```bash
./mvnw compile                # compila
./mvnw test                   # testes (precisa do Docker rodando, por causa do Testcontainers)
./mvnw spring-boot:run        # sobe a app (e o docker compose junto)
cd frontend && npm run dev    # sobe o Vite em :5173 (backend precisa estar rodando em :8080)
cd frontend && npm test       # testes do frontend
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
- Frontend: TypeScript em modo estrito, indentação de 2 espaços, componentes funcionais com hooks, estilo por classes utilitárias do Tailwind (sem SCSS), um módulo (`urlService.ts`, funções com `fetch`) concentra as chamadas HTTP; a UI nunca monta URLs da API fora dele
- Frontend: tipos espelham os DTOs do backend (`CreateUrlRequest`, `CreateUrlResponse`); erros 400 do backend (`ProblemDetail`) são exibidos na tela

## Design do frontend
- Escopo: **uma tela só** — campo de entrada da URL (mais `expiresInDays` opcional) e saída com o link curto e botão de copiar; erros 400 do backend aparecem na própria tela
- Componentes: usar os do neobrutalism.dev (Button, Input, Card, Alert etc.) em vez de escrever CSS de botão/campo do zero; personalizar pelo tema (variáveis CSS do shadcn em `index.css`), não componente por componente
- Estilo: **neobrutalismo** com visual retrô de papel creme, inspirado em https://7a0.com.br/ (copiar só a linguagem visual, nunca a identidade, os textos nem o tema do site): sombra dura sem desfoque, bordas pretas grossas, cantos quase retos (`border-radius: 2px`), cores chapadas, títulos grandes em caixa alta
- Fontes: **IBM Plex** — `IBM Plex Sans Condensed` 700 em maiúsculas nos títulos e botões, `IBM Plex Sans` no texto, `IBM Plex Mono` no link curto/código. Carregar por Google Fonts ou self-host (decidir ao criar o app)
- Paleta (sobrescrever as variáveis do tema do neobrutalism.dev em `:root`, no `index.css`): papel `#f3ecd8` (`--background`), tinta `#1b1a17` (`--foreground`/`--border`), destaque `#e8462b` (`--main`); sombra `3px 3px 0` na cor da tinta (`--shadow` do tema)
- Botão: o do neobrutalism.dev já tem sombra dura e efeito de apertar no `:active`; ajustar o deslocamento para 3px se o padrão for diferente
- Conferir o contraste do texto branco sobre o vermelho de destaque antes de fixar a cor

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
- URL inválida na criação: `@ValidHttpUrl` (`dto/ValidHttpUrl` + `HttpUrlValidator`) faz `URI.create` e exige esquema http/https e host; URL com espaço agora dá 400 em vez de 500 no redirect. Log de colisão corrigido (`attempt + 1`, sem stack trace) e imports explícitos em `CreateUrlRequest` (PR #2)
- Testes unitários e de camada web (PR #2), usando só o que já vem nos starters `-test` (JUnit Jupiter, Mockito, AssertJ):
  - `ShortUrlServiceImplTest` (`@ExtendWith(MockitoExtension.class)`, `@Mock` + `@InjectMocks ShortUrlServiceImpl`; o tipo do `@InjectMocks` tem que ser a classe concreta, não a interface)
  - `ShortControllerTest` (`@WebMvcTest` + `@Import(GlobalExceptionHandler.class)` + `@MockitoBean ShortUrlService`)
  - No Spring Boot 4 `@MockBean` foi removido: usar `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`); `@WebMvcTest` fica em `org.springframework.boot.webmvc.test.autoconfigure`
  - `./mvnw test`: 13 testes passando. Rodar a suíte inteira, não só a classe `UrlShortenerApplicationTests`
- Mensagens das exceções em frase normal, teste de URL sem host (`http:///x`, cobre a precedência do `HttpUrlValidator`; validado com o ciclo vermelho/verde) e teste de retry de colisão com sucesso; `./mvnw test`: 15 testes (PR #3)
- Cache no Redis do `resolve` (PR #4): `ShortUrlCache` (`StringRedisTemplate`, chave `short-url:<code>` → URL), cache-aside, TTL = menor entre o tempo até `expiresAt` e 1 hora; falhas do Redis são logadas e ignoradas (fallback para o banco). `registerHit` passou a ser por `code` (acerto de cache não tem o `id`). `application.yaml` com `timeout`/`connect-timeout` de 200ms: sem isso, com o Redis caindo com a app já conectada, o Lettuce enfileira o comando por ~60s e o redirect trava. `./mvnw test`: 22 testes
- Validado à mão com Redis real: chave criada com TTL ≈ 3600s e redirect funcionando com o Redis parado (app subindo sem Redis)
- CI com GitHub Actions (PR #6): `.github/workflows/ci.yml` roda `./mvnw -B test` (Temurin 25, cache do Maven) em `push` e `pull_request` para o `main`; o `mvnw` foi marcado como executável no git (`git update-index --chmod=+x`). O `main` está protegido por ruleset: exige PR e o check `test` verde, com o branch atualizado; sem bypass, então até commits de docs passam por branch + PR
- Erros de validação (400): `GlobalExceptionHandler` trata `MethodArgumentNotValidException` e devolve `ProblemDetail` (`application/problem+json`) com `errors` (mapa campo → primeira mensagem), ex.: `{"detail":"Invalid request","status":400,"errors":{"url":"must be a valid http/https URL"}}`. Antes disso o 400 vinha com o corpo genérico do Spring Boot, sem dizer o campo. Documentado no Swagger e coberto no `ShortControllerTest` (`errors.url`, `errors.expiresInDays`)
## Problemas conhecidos
1. **Máquina do trabalho**: `./mvnw spring-boot:run` iniciado pelo Claude falha com `Unable to establish loopback connection` ao subir o Tomcat (JVM/Windows, provável VPN/antivírus/EDR); não é problema do código. Pelo IntelliJ a app sobe normalmente. Testes com `MockMvc` não abrem porta e funcionam
2. O IntelliJ sugere trocar `http://` por `https://` em strings de teste; no teste `http:///x` isso desfaz a cobertura do bug de precedência do `HttpUrlValidator` (só o esquema `http` o exercita). Manter `http` (ideal: `@SuppressWarnings("HttpUrlsUsage")` + comentário)
3. Com o Redis fora, o health check do Redis do Actuator provavelmente deixa `/actuator/health` em `DOWN` (visto pela stack trace reativa no log; não verificado). Decidir na etapa de observabilidade se o Redis deve contar no health geral
4. O cache pode servir um link por até 1 hora depois de removido do banco; hoje não existe endpoint de remoção. Considerar invalidar a chave se ele for criado
5. JSON malformado no `POST /api/urls` (`HttpMessageNotReadableException`) também dá 400, mas fora do `GlobalExceptionHandler`: volta o corpo genérico do Spring, sem `errors`. O frontend sempre envia JSON válido, então fica como melhoria

## Próximos passos
1. **Observabilidade (versão simples)** (`feat/observability`): (a) expor `/actuator/prometheus` (`management.endpoints.web.exposure.include: health,prometheus` — hoje não há nada de `management` no `application.yaml`); (b) poucas métricas Micrometer via `MeterRegistry` no service: `urls.created`, `urls.redirects` com tag `result` (`hit`/`miss`/`not_found`/`expired`), cache hit/miss e colisões de código, testáveis com `SimpleMeterRegistry`; **nunca usar o `code` como tag** (alta cardinalidade); (c) serviço `prometheus` no `compose.yaml` com `prometheus.yml` raspando a app (app pelo IntelliJ no host: alvo `host.docker.internal:8080`; em Linux/WSL talvez `extra_hosts: host.docker.internal:host-gateway`); validar as consultas na UI do Prometheus (`localhost:9090`), ex.: `rate(urls_redirects_total[1m])`. Decidir aqui se o Redis deve contar no `/actuator/health` (problema conhecido nº 3)
2. **Grafana** (`feat/grafana`, depois do passo 2; faz parte da especificação base): configurar por **provisioning** (datasource e dashboard em JSON versionados, montados no contêiner, para o `docker compose up` já subir pronto), com 4 ou 5 painéis ligados às métricas do passo 1 (redirects por resultado, cache hit/miss, criações, colisões, latência do redirect). Se o tempo apertar, o Prometheus sozinho já cumpre a observabilidade
3. **Contagem de cliques assíncrona**: hoje `registerHit` (agora por `code`) é síncrono no caminho do redirect, tanto no acerto quanto no miss do cache, o que contradiz a especificação. Opções: `@Async`, ou contar no Redis (`INCR`) e sincronizar com o Postgres em lote por job agendado. Já nasce com métricas (cliques perdidos/falhas)
4. **Rate limiting com Bucket4j** apoiado no Redis (por IP), com resposta 429 e métrica de requisições rejeitadas
5. Testes de integração (`@SpringBootTest` + Testcontainers, cobrindo `registerHit` real e os cenários já validados à mão). Na contagem de cliques, considerar filtrar robôs e pré-visualizadores de link (WhatsApp, Slack) por `User-Agent`
6. **Frontend simples** (`feat/frontend`): criar o app em `frontend/` (Vite + React + TS, Tailwind, inicializar o shadcn e adicionar os componentes do neobrutalism.dev), proxy para o backend, tela única (ver "Design do frontend") com campo da URL (+ `expiresInDays` opcional), saída com o link curto e botão de copiar, e mensagens de erro da validação (400)
7. Testes do frontend: Vitest + React Testing Library: `urlService` com `fetch` mockado e o componente do formulário (inclusive a exibição do erro 400)
8. Opcional: exibir contagem de cliques no frontend (depende do endpoint de estatísticas por código, item 10)
9. Opcional: endpoint de estatísticas por código, limpeza de URLs expiradas
