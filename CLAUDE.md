# URL Shortener

Encurtador de URLs com métricas, feito com Java e Spring Boot. Projeto de aprendizado/portfólio.
O dono quer **entender** o código: explique decisões e trade-offs, e prefira mostrar o código para ele implementar manualmente quando pedir.

## Especificação base
- Redis para cache e rate limiting (Bucket4j)
- Contagem de cliques com processamento assíncrono
- Spring Actuator + Micrometer + Prometheus/Grafana para observabilidade

## Stack
- Java 25, Spring Boot 4.1.1, Maven (wrapper: `./mvnw`, dentro de `backend/`)
- PostgreSQL 15 + Liquibase (schema), Redis 7 (cache do `resolve`)
- `spring-boot-docker-compose`: ao subir a app, o `compose.yaml` (na raiz do repositório, apontado por `spring.docker.compose.file: ../compose.yaml`) sobe o Postgres e o Redis e a conexão é configurada automaticamente (por isso as portas são publicadas sem porta fixa no host)
- Já no `backend/pom.xml`: Actuator, Micrometer Prometheus, datasource-micrometer, validation, `spring-boot-starter-liquibase`, Testcontainers (`spring-boot-testcontainers` + `junit-jupiter` + `postgresql`, BOM em `1.21.4`)
- **Atenção (Spring Boot 4.x)**: a autoconfiguração do Liquibase foi modularizada — `org.liquibase:liquibase-core` sozinho só traz a ferramenta de migração, não o bean `SpringLiquibase`. É preciso o starter `spring-boot-starter-liquibase` para o Spring de fato rodar as migrations
- Testcontainers `1.20.x` não negocia corretamente a API do Docker com engines mais novos (erro "client version 1.32 is too old"); por isso o BOM está fixado em `1.21.4`
- Swagger UI via `springdoc-openapi-starter-webmvc-ui` `3.1.1` (linha do Spring Boot 4; versão fixada no `pom.xml`, fora do BOM do Boot)
- Spring Boot 4 usa **Jackson 3**: o pacote é `tools.jackson.databind` (`ObjectMapper`, `JsonNode`), não `com.fasterxml.jackson.databind`. Em testes, `@AutoConfigureMockMvc` vem de `org.springframework.boot.webmvc.test.autoconfigure`
- Frontend (pasta `frontend/`): React 19 + TypeScript 6 com Vite 8, Tailwind CSS e componentes do [neobrutalism.dev](https://www.neobrutalism.dev/) (construído sobre shadcn/ui: os componentes são **copiados** para o projeto, em `src/components/ui/`, via CLI `npx shadcn@latest add <url do componente>`, e não instalados como pacote; podem ser editados à mão). Versões: Tailwind 4.3, shadcn CLI 4.21, `@base-ui/react` 1.9 (os componentes do neobrutalism.dev vieram sobre Base UI, não Radix). O alias `@` → `src` fica no `vite.config.ts` e no `paths` dos `tsconfig` (sem `baseUrl`, depreciado no TypeScript 6). `erasableSyntaxOnly` está ligado: sem parameter properties em construtores (declarar o campo na classe)
- Dev do frontend: `npm run dev` (Vite) em `http://localhost:5173` com `server.proxy` no `vite.config.ts` apontando `/api` para `http://localhost:8080`, para não exigir CORS no backend. O `shortUrl` é montado com o host da requisição: pelo proxy ele vem com a porta 5173 e o `GET /{code}` também precisaria passar pelo proxy; abrir o link curto sempre pela porta 8080

## Estrutura do repositório
- `backend/` (Spring Boot: `pom.xml`, `mvnw`, `.mvn`, `src/`), `frontend/` (Vite) e, na raiz, `compose.yaml` (infra compartilhada: Postgres/Redis e, depois, Prometheus/Grafana), `.github/` e `CLAUDE.md`. Movido no PR #13; o histórico segue com `git log --follow`
- Pacotes Java e caminhos citados abaixo (`src/main/...`, `application.yaml`) ficam sob `backend/`
- Ao rodar pelo IntelliJ, o working directory da run configuration deve ser `backend/` (o caminho `../compose.yaml` é relativo a ele)
- Neste shell o `JAVA_HOME` aponta para o JDK 21; para o Maven usar `~/.jdks/openjdk-25.0.2`

## Comandos
```bash
cd backend && ./mvnw compile  # compila
cd backend && ./mvnw test     # testes (precisa do Docker rodando, por causa do Testcontainers)
cd backend && ./mvnw spring-boot:run  # sobe a app (e o docker compose junto)
cd frontend && npm run dev    # sobe o Vite em :5173 (backend precisa estar rodando em :8080)
cd frontend && npm test       # testes do frontend (ainda não existe: vem com o Vitest, item 7)
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
- CI com GitHub Actions (PR #6): `.github/workflows/ci.yml` roda `./mvnw -B test` com `working-directory: backend` (Temurin 25, cache do Maven por `backend/pom.xml`) em `push` e `pull_request` para o `main`; o `mvnw` foi marcado como executável no git (`git update-index --chmod=+x`). O `main` está protegido por ruleset: exige PR e o check `test` verde, com o branch atualizado; sem bypass, então até commits de docs passam por branch + PR
- Erros de validação (400): `GlobalExceptionHandler` trata `MethodArgumentNotValidException` e devolve `ProblemDetail` (`application/problem+json`) com `errors` (mapa campo → primeira mensagem), ex.: `{"detail":"Invalid request","status":400,"errors":{"url":"must be a valid http/https URL"}}`. Antes disso o 400 vinha com o corpo genérico do Spring Boot, sem dizer o campo. Documentado no Swagger e coberto no `ShortControllerTest` (`errors.url`, `errors.expiresInDays`)
- Frontend (PRs #10 e #11): app Vite + React + TS em `frontend/`, Tailwind 4, shadcn inicializado, tema do neobrutalism.dev (`styling/blue.json`) com a paleta creme e a IBM Plex (`@fontsource`) já aplicadas no `index.css`; `utils.ts` com `cn()` própria (`clsx` + `tailwind-merge`) — o `init` do shadcn tinha instalado o pacote `cn` de terceiros, removido. `Button`, `Input`, `Card` e `Alert` em `components/ui/`; botões em caixa alta com a Condensed (`font-heading uppercase` no `buttonVariants`) e `Card` com fundo `--secondary-background`; regra `react-refresh/only-export-components` desligada para `src/components/ui/` no `eslint.config.js` (componentes copiados do shadcn exportam `*Variants`)
- Observabilidade simples (PR #15): `/actuator/prometheus` exposto (`management.endpoints.web.exposure.include: health, prometheus`) e Redis fora do health (`management.health.redis.enabled: false`: o cache é opcional, com fallback para o Postgres, então o Redis fora não deve derrubar o `/actuator/health`). Contadores Micrometer no `ShortUrlServiceImpl`: `urls.created`, `urls.code.collisions` e `urls.redirects` com tag `result` (`hit`/`miss`/`not_found`/`expired`; a taxa de acerto do cache sai de `hit` sobre o total, sem contador separado); testados com `@Spy MeterRegistry = new SimpleMeterRegistry()` (o `@InjectMocks` passaria `null` sem ele). Serviço `prometheus` no `compose.yaml` (porta fixa `9090`, `extra_hosts: host.docker.internal:host-gateway` para Linux/WSL) com `prometheus.yml` na raiz raspando `host.docker.internal:8080`. Validado à mão: `localhost:9090/targets` em `UP`. `./mvnw test`: 23 testes. O Spring Boot desliga a exportação de métricas em testes, então o `/actuator/prometheus` só se valida rodando a app
- `types/urls.ts` (espelha os DTOs) e `services/urlService.ts`: `createShortUrl` faz `POST /api/urls` e lança `ApiError` (com `fieldErrors` lidos de `errors` do 400); falha de rede e status inesperados viram `ApiError` com mensagem genérica. `npm run build` passa; `CreateUrlRequest.expiresInDays` é opcional (o DTO do backend é `Integer` anulável). Validado contra o backend real pelo proxy do Vite (fluxo feliz)
- `App.tsx`: tela única (PR #11) com URL + `expiresInDays` opcional, link curto com botão de copiar, erros de campo (`fieldErrors`) sob cada input e `Alert` para falhas sem campo (rede/500). O link usa `replace(":5173", ":8080")` como remendo de dev (em produção front e API compartilhariam o host). `npm run build` passa; o lint só ignora a pasta `ui/`. Validados à mão contra o backend real: fluxo feliz, URL > 2048 (`errors.url`), `ftp://x`, `expiresInDays` 0 (`errors.expiresInDays`) e backend parado (o proxy do Vite devolve 5xx, então aparece "Unexpected server error. Please try again.", não "Unable to communicate", que só ocorre se o próprio Vite cair). **Pendente**: conferir o contraste do branco sobre `--main` (~3.9:1: ok só para texto grande/negrito)
## Problemas conhecidos
1. **Máquina do trabalho**: `./mvnw spring-boot:run` iniciado pelo Claude falha com `Unable to establish loopback connection` ao subir o Tomcat (JVM/Windows, provável VPN/antivírus/EDR); não é problema do código. Pelo IntelliJ a app sobe normalmente. Testes com `MockMvc` não abrem porta e funcionam
2. ~~IntelliJ sugerindo `https://` nos testes~~ (resolvido: `@SuppressWarnings("HttpUrlsUsage")` + comentário em `create_rejectsHttpUrlWithoutHost`; manter `http` ali, é o esquema que exercita a checagem de host do `HttpUrlValidator`)
3. ~~Health do Redis~~ (resolvido no PR #15 e verificado na prática: `/actuator/health` continua `UP` com o Redis parado)
4. O cache pode servir um link por até 1 hora depois de removido do banco; hoje não existe endpoint de remoção. Considerar invalidar a chave se ele for criado. Resolver junto do endpoint de remoção (item 9 dos próximos passos): `ShortUrlCache.evict(code)` chamado depois de apagar do banco; se o Redis estiver fora, a entrada expira pelo TTL
5. ~~JSON malformado~~ (resolvido: `HttpMessageNotReadableException` tratada no `GlobalExceptionHandler`, devolve `ProblemDetail` com `detail: "Malformed JSON request"`, sem `errors` e sem vazar a mensagem do Jackson; o frontend mostra no `Alert`)
6. ~~Mensagens de validação em idiomas misturados~~ (resolvido: `message` explícita em todas as constraints de `CreateUrlRequest`, em inglês, coberto no `ShortControllerTest`)

## Próximos passos
1. ~~Observabilidade (versão simples)~~ (feito, PR #15)
2. **Grafana** (`feat/grafana`, depois do passo 1; faz parte da especificação base): configurar por **provisioning** (datasource e dashboard em JSON versionados, montados no contêiner, para o `docker compose up` já subir pronto), com 4 ou 5 painéis ligados às métricas do passo 1 (redirects por resultado, cache hit/miss, criações, colisões, latência do redirect). Se o tempo apertar, o Prometheus sozinho já cumpre a observabilidade
3. **Contagem de cliques assíncrona**: hoje `registerHit` (agora por `code`) é síncrono no caminho do redirect, tanto no acerto quanto no miss do cache, o que contradiz a especificação. Opções: `@Async`, ou contar no Redis (`INCR`) e sincronizar com o Postgres em lote por job agendado. Já nasce com métricas (cliques perdidos/falhas)
4. **Rate limiting com Bucket4j** apoiado no Redis (por IP), com resposta 429 e métrica de requisições rejeitadas
5. Testes de integração (`@SpringBootTest` + Testcontainers, cobrindo `registerHit` real e os cenários já validados à mão). Na contagem de cliques, considerar filtrar robôs e pré-visualizadores de link (WhatsApp, Slack) por `User-Agent`
6. ~~Frontend simples~~ (feito, PR #11, validado à mão)
7. Testes do frontend: Vitest + React Testing Library: `urlService` com `fetch` mockado e o componente do formulário (inclusive a exibição do erro 400)
8. Opcional: exibir contagem de cliques no frontend (depende do endpoint de estatísticas por código, item 10)
9. Opcional: endpoint de estatísticas por código, limpeza de URLs expiradas
