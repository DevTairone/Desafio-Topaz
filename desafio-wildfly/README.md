# Encurtador de URLs — versão WildFly 10 / Java EE 7

Mesmo serviço do módulo `desafio/` (recebe uma URL, devolve uma versão encurtada, redireciona ao acessá-la), mas reescrito para atender à premissa **"deve rodar em WildFly 10"**, que é incompatível com Java 21 + Spring Boot usados no módulo original (WildFly 10 é de 2016, suporta só até Java EE 7 / Servlet 3.1 / namespace `javax.*`).

Este módulo não usa Spring: é Java EE 7 "puro" — **JAX-RS** para os endpoints, **CDI** para injeção de dependência, **JPA** (via `EntityManager` direto, sem Spring Data) para persistência — tudo já embutido no próprio WildFly, sem precisar adicionar essas dependências.

## Por que um módulo separado

Os dois módulos resolvem o mesmo requisito de negócio de formas tecnicamente incompatíveis entre si. Em vez de descartar o trabalho já feito em Spring Boot (que continua sendo a solução mais moderna/idiomática caso a premissa do WildFly não se aplique de fato), mantive os dois lado a lado:

- `desafio/` — Java 21 + Spring Boot (Clean Architecture, Swagger, testes de integração completos).
- `desafio-wildfly/` — Java 8 + Java EE 7 puro, deployável em WildFly 10.

## Pré-requisitos

- Java 8 (para compilar e para o WildFly 10 rodar)
- WildFly 10.1.0.Final (download manual) **ou** Docker

## Como buildar

```powershell
./gradlew war
```

Gera `build/libs/desafio-wildfly.war`.

## Como rodar

### Opção 1: Docker (mais simples)

```powershell
docker compose up --build
```

Isso builda a imagem (a partir do WAR já gerado por `./gradlew war` — rode esse comando antes) e sobe o WildFly com o datasource H2 (`docker/h2-ds.xml`) já configurado. Acesse em `http://localhost:8080`.

### Opção 2: WildFly instalado localmente

1. Baixe e extraia o [WildFly 10.1.0.Final](https://www.wildfly.org/downloads/).
2. Copie `docker/h2-ds.xml` para `<wildfly>/standalone/deployments/`.
3. Copie `build/libs/desafio-wildfly.war` para `<wildfly>/standalone/deployments/`.
4. Inicie o servidor: `<wildfly>/bin/standalone.sh` (ou `standalone.bat` no Windows).
5. Acesse `http://localhost:8080`.

## Datasources (dev/prod)

O `persistence.xml` sempre aponta para o mesmo JNDI (`java:/DesafioDS`) — o que muda entre ambientes é **qual datasource o servidor tem registrado sob esse nome**, não o código da aplicação:

- **Dev**: `docker/h2-ds.xml` — H2 em memória, já pronto para uso, sem instalar nada a mais.
- **Prod**: `docker/postgres-ds.xml` — Postgres. Antes de usá-lo, copie o jar do driver JDBC do Postgres (ex.: `postgresql-42.7.3.jar`) para `standalone/deployments/` também, e ajuste host/porta/banco/credenciais no arquivo.

## Endpoints

Mesmos contratos da versão Spring Boot (JSON, mesmos campos e códigos de status):

- `POST /api/shorten` — corpo `{ "url": "https://..." }`, resposta `201 Created`.
- `GET /{codigo}` — `302 Found` com header `Location`.
- Erros: `400` (URL inválida), `404` (código não encontrado), `410` (expirado/desativado).

## Principais diferenças em relação ao módulo Spring Boot

| Conceito | Spring Boot (`desafio/`) | WildFly / Java EE 7 (`desafio-wildfly/`) |
|---|---|---|
| Endpoints HTTP | Spring MVC (`@RestController`) | JAX-RS (`@Path`/`@GET`/`@POST`) |
| Injeção de dependência | Spring (`@Service`, `@Autowired`) | CDI (`@ApplicationScoped`, `@Inject`) |
| Persistência | Spring Data JPA (repositórios por interface) | JPA puro via `EntityManager` (DAOs em `infrastructure.persistence`) |
| Transações | `@Transactional` do Spring | `@Transactional` do JTA (`javax.transaction`) |
| Tratamento de erros | `@RestControllerAdvice` | `ExceptionMapper` (JAX-RS `@Provider`) |
| Configuração de ambiente | `application-{dev,prod}.yml` (perfis Spring) | `persistence.xml` fixo + datasource JNDI trocado no servidor |
| Empacotamento | JAR executável (servidor embutido) | WAR deployado em um WildFly externo |

## Limitações conhecidas deste módulo

- **Sem Swagger/OpenAPI.** O springdoc-openapi usado no outro módulo é específico do Spring; documentar via OpenAPI aqui exigiria outra biblioteca (ex.: Swagger Core com configuração manual de scanner), o que não foi feito — ficou fora do escopo desta adaptação.
- **Sem teste de integração ponta a ponta automatizado.** Rodar um teste que sobe o WAR de fato dentro de um WildFly exigiria Arquillian (framework de testes em container), uma dependência e uma configuração adicionais significativas. Os testes aqui cobrem a lógica de negócio isoladamente (Mockito), na mesma cobertura do módulo Spring — a validação end-to-end real precisa ser manual (deploy + `curl`/Postman) ou via uma suíte Arquillian a ser adicionada depois.
- **Compilado para Java 8**, já que é o que o WildFly 10 suporta — não usa nenhuma sintaxe de Java 9+ (sem `var`, sem `Stream#toList()`, sem `Optional#orElseThrow()` sem argumento, etc.).
