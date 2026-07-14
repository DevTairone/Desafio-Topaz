# Encurtador de URLs

Serviço REST que recebe uma URL e devolve uma versão encurtada; ao acessar a URL encurtada, redireciona automaticamente para a URL original.

Para detalhes de arquitetura, veja [ARCHITECTURE.md](ARCHITECTURE.md).

## Decisões técnicas e trade-offs

Registro aqui o porquê das principais escolhas do projeto, não só o que foi feito.

- **Motor de geração serializado com `ExecutorService` de thread única, em vez de um simples `synchronized`.** Isso cria uma fila explícita de geração, desacoplada das threads HTTP: toda chamada é submetida à fila e bloqueia até ser processada, garantindo uma requisição por vez independentemente de quantas cheguem ao mesmo tempo. Também isola essa lógica num único ponto, mais fácil de testar (veja o teste de concorrência dedicado).
- **Código curto gerado a partir de um contador dedicado (`UrlCounter`), não do id auto-incrementado da entidade `ShortUrl`.** Desacopla o código exposto publicamente do id técnico da linha no banco, e permite reforçar a atomicidade também no nível do banco via lock pessimista (`SELECT ... FOR UPDATE`) — uma segunda camada de segurança além da serialização em memória, útil se a aplicação rodar futuramente com múltiplas instâncias.
- **Organização em Clean Architecture (`domain` → `application` → `infrastructure`).** Deixa explícito o que é regra de negócio pura, o que é caso de uso e o que é detalhe de entrega HTTP. Trade-off assumido conscientemente: os repositórios ficam em `domain.repository` mesmo estendendo `JpaRepository` do Spring Data — simplificação pragmática comum em projetos Spring Boot, não é Clean Architecture 100% "pura" (o ideal "de livro" seria um port agnóstico de framework em `domain` com um adapter em `infrastructure`).
- **Schema gerenciado pelo Hibernate (`ddl-auto=update`) em vez de Flyway/Liquibase.** Redução de escopo deliberada: controle de schema versionado adicionaria complexidade sem agregar ao requisito central do desafio. Fica registrado como próximo passo natural antes de um ambiente de produção real.
- **`accessCount` incrementado sem lock no redirecionamento.** O requisito de "uma requisição por vez" pedido era especificamente para o motor de geração. Optei por não estender essa serialização ao caminho de redirecionamento (que deve continuar rápido e paralelo por natureza), aceitando que essa métrica secundária pode ter pequena imprecisão sob concorrência alta — troca consciente entre desempenho de leitura e exatidão de uma contagem que não é crítica para o negócio.
- **Perfis `dev` (H2) / `prod` (PostgreSQL) via Spring Profiles + Docker Compose.** Permite rodar e testar tudo localmente sem nenhuma dependência externa (H2 em memória), e ainda validar contra um banco real antes de ir para produção, sem mudar código — só a variável de ambiente do perfil.

## Pré-requisitos

- Java 21
- Docker (opcional — só necessário para rodar com PostgreSQL)

O projeto usa o Gradle Wrapper, então não é preciso instalar o Gradle separadamente.

## Como rodar

### Opção 1: perfil de desenvolvimento (H2 em memória, sem dependências externas)

```powershell
./gradlew bootRun
```

A aplicação sobe em `http://localhost:8080` com o perfil `dev` ativo por padrão. Não precisa de Docker nem de configuração adicional.

Console do H2 disponível em `http://localhost:8080/h2-console`:
- JDBC URL: `jdbc:h2:mem:desafio`
- Usuário: `sa`
- Senha: (em branco)

### Opção 2: perfil de produção (PostgreSQL via Docker)

1. Suba o banco:
   ```powershell
   docker compose up -d
   ```
2. Rode a aplicação apontando para o perfil `prod` (as credenciais precisam bater com as do `docker-compose.yml`):
   ```powershell
   $env:SPRING_PROFILES_ACTIVE="prod"
   $env:DB_PASSWORD="desafio"
   ./gradlew bootRun
   ```

## Documentação interativa (Swagger)

Com a aplicação rodando:
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Especificação OpenAPI (JSON): http://localhost:8080/v3/api-docs

## Endpoints

### Encurtar uma URL

`POST /api/shorten`

Corpo da requisição:
```json
{
  "url": "https://exemplo.com/uma-pagina-com-url-bem-longa"
}
```

Resposta (`201 Created`):
```json
{
  "shortUrl": "http://localhost:8080/1",
  "shortCode": "1",
  "originalUrl": "https://exemplo.com/uma-pagina-com-url-bem-longa",
  "createdAt": "2026-07-14T10:00:00",
  "expiresAt": null
}
```

Exemplo com `curl`:
```powershell
curl -X POST http://localhost:8080/api/shorten `
  -H "Content-Type: application/json" `
  -d '{\"url\":\"https://exemplo.com\"}'
```

Se a mesma URL for enviada novamente, o serviço reaproveita o código já gerado (não cria um novo).

### Acessar/redirecionar

`GET /{codigo}`

Responde `302 Found` com o header `Location` apontando para a URL original.

```powershell
curl -i http://localhost:8080/1
```

## Erros

| Status | Quando acontece |
|---|---|
| `400 Bad Request` | URL vazia, malformada, ou com protocolo diferente de `http`/`https` |
| `404 Not Found` | O código informado não existe |
| `410 Gone` | O código existe, mas a URL está expirada ou desativada |
| `500 Internal Server Error` | Falha inesperada no processamento |

Corpo padrão de erro:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Código curto não encontrado: abc123",
  "timestamp": "2026-07-14T10:00:00"
}
```

## Rodando os testes

```powershell
./gradlew test
```

Inclui testes unitários e um teste de integração ponta a ponta (`UrlShortenerIntegrationTest`) que sobe o contexto real da aplicação com H2 e valida o fluxo completo via HTTP.

## Estrutura do projeto

O código é organizado em Clean Architecture (`domain` → `application` → `infrastructure`). Veja [ARCHITECTURE.md](ARCHITECTURE.md) para a explicação completa de cada camada e das decisões de design (motor de geração serializado, contador com lock pessimista, tratamento de erros, etc.).
