# Arquitetura e decisões de projeto

Este documento descreve o que foi construído neste serviço de encurtamento de URLs e por quê, para quem for dar manutenção ou continuar o desenvolvimento.

## Visão geral

O serviço recebe uma URL original, gera um código curto único para ela e devolve a URL encurtada. Ao acessar a URL encurtada (`GET /{codigo}`), o serviço redireciona (HTTP 302) para a URL original.

Requisito central do desafio: **o motor de geração de códigos deve processar apenas uma requisição por vez, de forma sincronizada**, mesmo que múltiplas requisições cheguem concorrentemente.

## Stack utilizada

- Java 21
- Spring Boot 4.1.0 (Spring MVC via `spring-boot-starter-webmvc`, Spring Data JPA)
- H2 (perfil dev) / PostgreSQL (perfil prod)
- Lombok
- JUnit 5 + Mockito + MockMvc
- springdoc-openapi (Swagger UI)
- Gradle (com wrapper)
- Docker Compose (para subir o PostgreSQL localmente)

## Organização em Clean Architecture

```
com.sys_top.desafio
├── domain                 # regras de negócio puras, sem dependência de framework web
│   ├── model               (ShortUrl, UrlCounter, UrlStatus)
│   ├── repository           (contratos: ShortUrlRepository, UrlCounterRepository)
│   └── exception            (UrlGenerationException, InvalidUrlException,
│                             ShortUrlNotFoundException, ShortUrlExpiredException)
├── application             # casos de uso / orquestração
│   ├── UrlGenerationEngine (+ Impl)
│   ├── Base62Encoder
│   ├── UrlCounterService
│   ├── UrlRedirectService
│   └── UrlValidator
└── infrastructure          # adaptadores de entrega (frameworks e drivers)
    ├── web                  (UrlShortenerController, RedirectController,
    │                         GlobalExceptionHandler, dto/)
    └── config               (OpenApiConfig)
```

A regra de dependência é sempre para dentro: `infrastructure` depende de `application`, que depende de `domain`; `domain` não depende de nada das outras camadas.

**Trade-off assumido:** os repositórios (`ShortUrlRepository`, `UrlCounterRepository`) ficam em `domain.repository` mesmo estendendo `JpaRepository` do Spring Data. Isso é uma simplificação pragmática comum em projetos Spring Boot, não é Clean Architecture 100% pura — o ideal "por livro" seria um port agnóstico de framework em `domain` com um adapter implementando-o em `infrastructure.persistence`. Não foi feito por ser uma mudança estrutural maior do que o escopo pedido.

## Modelagem de domínio

- **ShortUrl**: `id`, `shortCode`, `originalUrl`, `createdAt`, `expiresAt`, `accessCount`, `status` (`ACTIVE`/`EXPIRED`/`DISABLED`). Contém as regras `isExpired()`, `isRedirectable()` e `incrementAccessCount()`.
- **UrlCounter**: linha única (id fixo = 1) que guarda o valor atual do contador global usado para gerar os códigos.

## Motor de geração (o requisito de serialização)

`UrlGenerationEngineImpl` usa um `ExecutorService` de **thread única** como fila de processamento: toda chamada a `generate(url)` é submetida a essa fila e o método bloqueia (`Future.get()`) até a tarefa ser concluída. Isso garante execução estritamente sequencial (uma requisição de geração por vez, em ordem de chegada) mesmo que várias threads HTTP cheguem ao mesmo tempo.

Dentro dessa única worker thread, cada geração faz:
1. verifica se a URL já foi encurtada antes e ainda está ativa — se sim, reaproveita o código existente;
2. busca o próximo valor do contador (`UrlCounterService.nextValue()`);
3. codifica o valor em base62 (`Base62Encoder`);
4. persiste o `ShortUrl` já com o `shortCode` definido.

## Persistência do contador

`UrlCounterService` mantém uma única linha na tabela `url_counter` e a incrementa dentro de uma transação com **lock pessimista de escrita** (`@Lock(PESSIMISTIC_WRITE)`, equivalente a `SELECT ... FOR UPDATE`). Isso reforça a atomicidade também no nível do banco — relevante caso a aplicação venha a rodar com múltiplas instâncias no futuro, além da serialização em memória do motor. A linha do contador é criada automaticamente na subida da aplicação (`@PostConstruct`).

## Camada REST

- `POST /api/shorten`: valida a URL (`UrlValidator`: não vazia, sintaticamente válida, protocolo http/https), chama o motor de geração e devolve `201 Created` com a URL encurtada completa.
- `GET /{codigo}`: `UrlRedirectService` resolve o código (existência + status/expiração), incrementa `accessCount` e devolve `302 Found` com o header `Location` apontando para a URL original.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) traduz as exceções de domínio em respostas HTTP: `400` (URL inválida), `404` (código não encontrado), `410` (código expirado/desativado), `500` (erro de geração/inesperado).

## Configuração de ambiente

- `application.yml`: configuração comum (perfil ativo por padrão `dev`, porta, Actuator restrito a `health`/`info`).
- `application-dev.yml`: H2 em memória, `ddl-auto=update`, console H2 em `/h2-console`, logs em `DEBUG`.
- `application-prod.yml`: PostgreSQL via variáveis de ambiente `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`, logs em `INFO`.
- `docker-compose.yml`: sobe um PostgreSQL local para testar o perfil `prod`, com as mesmas variáveis usadas em `application-prod.yml`.

## Estratégia de testes

- **Unitários** (Mockito), um por classe de `application`/`domain`: geração de código, contador, validação de URL, resolução de redirecionamento.
- **Teste de concorrência dedicado** (`UrlGenerationEngineImplTest`): dispara 20 requisições simultâneas contra o motor e comprova que o pico de execuções simultâneas no repositório nunca passa de 1 — validação direta do requisito de serialização.
- **Testes de controller** com `MockMvc` (`standaloneSetup`), isolados via mocks das dependências.
- **Teste de integração ponta a ponta** (`UrlShortenerIntegrationTest`): sobe o contexto real da aplicação (perfil `dev`/H2, sem mocks) e usa `MockMvc` contra os controllers reais — cobre o fluxo completo de encurtar + redirecionar, reaproveitamento de código, erros HTTP, incremento de acessos e unicidade de códigos sob 30 requisições concorrentes reais.

## Documentação interativa

`springdoc-openapi` expõe Swagger UI em `/swagger-ui/index.html` e a especificação OpenAPI em `/v3/api-docs`.

## Limitações conhecidas / próximos passos possíveis

- **Schema gerenciado pelo Hibernate** (`ddl-auto=update`), sem Flyway/Liquibase — aceitável neste escopo, não recomendado para produção real.
- **`accessCount` pode sofrer race condition** sob concorrência real no redirecionamento (o requisito de serialização pedido era só para o motor de geração, não para o caminho de redirecionamento).
- **Contador e persistência do `ShortUrl` não são atômicos entre si** (duas transações separadas) — se a segunda falhar depois do contador já ter incrementado, fica um "buraco" na sequência (não causa duplicidade, só um número pulado).
- Sem rate limiting.
- Sem interface web (HTML) para o usuário digitar a URL — hoje é só API REST (ver `README.md` para exemplos de uso via `curl`/Swagger).
