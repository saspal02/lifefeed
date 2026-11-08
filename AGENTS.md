# Backend — Spring Boot

Standards: [`docs/java-style.md`](docs/java-style.md) · [`docs/annotations.md`](docs/annotations.md) ·
[`docs/layered-architecture.md`](docs/layered-architecture.md) · [`docs/controllers.md`](docs/controllers.md) ·
[`docs/mappers.md`](docs/mappers.md) · [`docs/exceptions.md`](docs/exceptions.md) ·
[`docs/testing.md`](docs/testing.md) · [`docs/logging.md`](docs/logging.md) ·
[`docs/observability.md`](docs/observability.md) · [`docs/database.md`](docs/database.md) ·
[`docs/security.md`](docs/security.md) · [`docs/agentic-workflows.md`](docs/agentic-workflows.md)

**Before any change:** read the topic file that matches the task (table below).

## Stack

Java 25 · Spring Boot 4.1.x · Maven · Spring Data JPA · PostgreSQL 18 · Flyway · Spring Security + JWT
(jjwt) · springdoc-openapi · Spring Modulith · Testcontainers · JUnit 6 + Mockito + AssertJ ·
Spotless + Palantir Java Format · ArchUnit (Taikai) · JaCoCo.

No Lombok. DTOs are records; entities use hand-written accessors; loggers are hand-written
(`LoggerFactory`).

Product context lives in [`docs/PRD.md`](docs/PRD.md).

## How to reason

- Answer as a senior engineer teaching a junior: technical, but never skipping the _why_.
- When you make a design choice, say in two or three lines: what you chose, the alternative
  you rejected, the mechanism that makes it better, and what breaks if it is chosen wrongly.
  Link the primary source (framework docs, RFC). "Best practice" alone is not a reason.
- Push back when a request is technically worse than an alternative, and state your
  confidence.
- Read the existing code before adding a pattern. If the code and these docs disagree, say so
  and follow the docs for new work rather than copying the debt.

## Task routing

| When the task is…                                      | Read first                                                       |
| ------------------------------------------------------ | ---------------------------------------------------------------- |
| Add or change an endpoint, entity or service           | `docs/layered-architecture.md`, `docs/controllers.md`            |
| Map between DTOs, models and entities                  | `docs/mappers.md`                                                |
| Add an error path                                      | `docs/exceptions.md`                                             |
| Add logging, an `@Async` path or an outgoing HTTP call | `docs/logging.md`, `docs/observability.md`                       |
| Write or fix tests                                     | `docs/testing.md`                                                |
| Add or change a table, column, index or constraint     | `docs/database.md`                                               |
| Touch auth, JWT, CORS, secrets or endpoint access      | `docs/security.md`                                               |
| Prepare a commit or delegate work to subagents         | `docs/agentic-workflows.md`                                      |

## Feature package layout

One package per feature, split into layer subpackages:

```
dev/saswat/lifefeed/<feature>/
  controller/   <Feature>Controller     @RestController @RequestMapping("/api/<feature>")
  service/      <Feature>Service        interface — the feature's public surface
                <Feature>ServiceImpl    @Service, class-level @Transactional — the one impl
  repository/   <Feature>Repository     interface extends JpaRepository<<Feature>Entity, Long>
  model/        <Feature>, enums        plain POJO, no jakarta.persistence imports
  entity/       <Feature>Entity         @Entity only, no logic
  dto/          Create/Update/Response records
  mapper/       <Feature>Mapper         static — DTO ↔ model
                <Feature>EntityMapper   static — model ↔ entity
```

Not every feature needs every file — a read-only feature has no `Create<Feature>Dto`. Add a
layer only when it actually carries weight.

## Current state

`users` is still a flat package (`dev.saswat.lifefeed.users`): controllers hold nested
request/response records, `UserService` is a concrete class, mapping is inline, and errors use
ad-hoc exception classes. Treat the layout above as the target for new features and for the
next refactor; don't replicate the flat structure in new features.

## Gotchas

- The service is an interface `<Feature>Service` plus one `@Service` implementation
  `<Feature>ServiceImpl` (both in `service/`). Inject and mock the interface; `@InjectMocks`
  in the service's own unit test targets `<Feature>ServiceImpl` — Mockito can't instantiate
  an interface.
- The repository is a plain Spring Data interface, nothing hand-written. "Find or 404" is a
  service concern:
  `repository.findById(id).orElseThrow(() -> new AppException(UserErrorMessage.USER_NOT_FOUND))`.
- `model/` holds plain POJOs (no JPA imports); `entity/` holds `@Entity` classes only, no
  logic. Never return an entity from a controller.
- **No `final` on method parameters or local variables, anywhere.** `final` survives only on
  constructor-injected fields (`private final UserRepository userRepository;`).
- `var` in controllers and tests; explicit types in services, mappers and the rest of
  production code.
- No existence checks or business logic in controllers — that belongs in the service, which
  should throw when something isn't there.
- `@Transactional` at class level, service classes only (`readOnly = true` for read paths).
- DTOs are records. Do **not** add Lombok — if you want it, raise it as a separate decision
  first.
- Never log personal data: no email, name, password, token or IP address. The current email
  log lines in the auth/user controllers are debt and should be removed when the file is
  touched.
- `WebSecurityConfig` currently ends with `.anyRequest().permitAll()`. That is known debt, not
  a pattern: new endpoints must be explicitly secured, and the chain should eventually become
  authenticated-by-default. See `docs/security.md`.
- Flyway owns the schema (`ddl-auto=validate`). Never edit an applied migration; add the next
  `V00N__*.sql`.
- Tests use Testcontainers PostgreSQL; there is no H2 profile. A test that needs a database
  extends `BaseIT` or uses `@DataJpaTest` with the existing container config.
- ArchUnit enforces the naming conventions: controllers end with `Controller`, services with
  `Service`, repositories with `Repository`, configuration classes with `Config`, and loggers
  are `private static final Logger LOG`.

## Build and quality gate

- **Full build with tests**: `./mvnw clean verify`
- **Single test class**: `./mvnw test -Dtest=dev.saswat.lifefeed.users.UserServiceTests`
- **Run application**: `./mvnw spring-boot:run`
- **Format code**: `./mvnw spotless:apply`
- **Format check**: `./mvnw spotless:check`
- **Docker image build**: `./mvnw clean compile spring-boot:build-image -DskipTests`

`clean verify` already runs Spotless during `compile` and the JaCoCo 40% line-coverage check
during `verify`. The minimum local gate before a commit is
`./mvnw spotless:check && ./mvnw test`; CI runs `./mvnw -ntp verify`.

## Commit and pull request guidelines

### Commit messages

- **Format**: `type(scope): description`
- **Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
- **Examples**:
  - `feat(auth): add JWT token refresh functionality`
  - `fix(api): handle null pointer in user service`
  - `test(users): add service unit tests for duplicate email`

### Pull requests

- **Branch naming**: `feature/description`, `fix/issue-number-description`
- **Title**: clear, concise description of changes
- **Description**: what changed and why, plus the spec/issue link when one exists
- **Testing**: all gates pass; state the commands you ran
- **Code review**: address all reviewer feedback
