# Backend — Spring Boot

Standards: [`docs/java-style.md`](docs/java-style.md) · [`docs/annotations.md`](docs/annotations.md) ·
[`docs/layered-architecture.md`](docs/layered-architecture.md) · [`docs/controllers.md`](docs/controllers.md) ·
[`docs/mappers.md`](docs/mappers.md) · [`docs/exceptions.md`](docs/exceptions.md) ·
[`docs/testing.md`](docs/testing.md) · [`docs/logging.md`](docs/logging.md) ·
[`docs/observability.md`](docs/observability.md) · [`docs/agentic-workflows.md`](docs/agentic-workflows.md)

**Before any change:** read the topic file that matches the task (table below).

## Stack

Java 25 · Spring Boot 4.x · Maven or Gradle · Spring Data JPA · Lombok · JUnit 6 + Mockito + AssertJ.

## How to reason

- Answer as a senior engineer teaching a junior: technical, but never skipping the _why_.
- When you make a design choice, say in two or three lines: what you chose, the alternative
  you rejected, the mechanism that makes it better, and what breaks if it is chosen wrongly.
  Link the primary source (framework docs, RFC). "Best practice" alone is not a reason.
- Push back when a request is technically worse than an alternative, and state your
  confidence.

## Task routing

| When the task is…                                      | Read first                                            |
| ------------------------------------------------------ | ----------------------------------------------------- |
| Add or change an endpoint, entity or service           | `docs/layered-architecture.md`, `docs/controllers.md` |
| Map between DTOs, models and entities                  | `docs/mappers.md`                                     |
| Add an error path                                      | `docs/exceptions.md`                                  |
| Add logging, an `@Async` path or an outgoing HTTP call | `docs/logging.md`, `docs/observability.md`            |
| Write or fix tests                                     | `docs/testing.md`                                     |
| Prepare a commit                                       | `docs/agentic-workflows.md`                           |

## Feature package layout

One package per feature, split into layer subpackages:

```
com/<company>/<app>/<feature>/
  controller/   <Feature>Controller     @RestController @RequestMapping("/<feature>")
  service/      <Feature>Service        interface — the feature's public surface
                <Feature>ServiceImpl    @Service, class-level @Transactional — the one impl
  repository/   <Feature>Repository     interface extends JpaRepository<<Feature>Entity, Id>
  model/        <Feature>, enums        plain POJO (Lombok), no jakarta.persistence imports
  entity/       <Feature>Entity         @Entity only, no logic
  dto/          Create/Update/Response records
  mapper/       <Feature>Mapper         static — DTO ↔ model
                <Feature>EntityMapper   static — model ↔ entity
```

Not every feature needs every file — a read-only feature has no `Create<Feature>DTO`. Add a
layer only when it actually carries weight.

## Gotchas

- The service is an interface `<Feature>Service` plus one `@Service` implementation
  `<Feature>ServiceImpl` (both in `service/`). Inject and mock the interface; `@InjectMocks`
  in the service's own unit test targets `<Feature>ServiceImpl` — Mockito can't instantiate
  an interface.
- The repository is a plain Spring Data interface, nothing hand-written. "Find or 404" is a
  service concern:
  `repository.findById(id).orElseThrow(() -> new AppException(AppErrorMessage.<X>_NOT_FOUND))`.
- `model/` holds plain POJOs (no JPA imports); `entity/` holds `@Entity` classes only, no logic.
- **No `final` on method parameters or local variables, anywhere.** Production code,
  controllers and tests all agree on this; `final` survives only on Lombok
  constructor-injected fields, because `@RequiredArgsConstructor` needs it.
- `var` in controllers and tests; explicit types in services and mappers.
- No existence checks or business logic in controllers — that belongs in the service, which
  should throw when something isn't there.
- `@Transactional` at class level, service classes only (`readOnly = true` for read paths).
