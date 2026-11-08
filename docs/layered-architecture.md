# Layered Architecture

Each feature is one package, split into layers. The table is the contract for what each layer
is allowed to touch — and, just as importantly, what it is not.

| Layer        | Responsibility                                                                                       | May import                                                              | Must NOT                                                                 |
| ------------ | ---------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| `controller` | bind + validate the request, call one service method, map the result to a DTO, set the status        | `service`, `dto`, `mapper`, `model`                                     | touch `repository` / `entity`; contain branching business logic; check existence |
| `service`    | the feature's logic: transactions, not-found, uniqueness checks, `Pageable`/`Sort`, orchestration     | `repository`, `model`, `mapper`, other features' `service` + `dto`       | import a controller; import another feature's `repository` / `entity`    |
| `repository` | persistence — Spring Data interfaces only                                                            | `entity`                                                                | be a hand-written class; hold logic beyond derived queries / `@Query`    |
| `model`      | the in-memory representation the service works with                                                  | other `model` types                                                     | import `jakarta.persistence.*`, Spring, or a DTO                          |
| `entity`     | the database row                                                                                     | `jakarta.persistence.*`, `shared/BaseEntity`                            | hold behaviour; be returned from a controller                            |
| `dto`        | the wire shape (request / response)                                                                  | —                                                                       | diverge from the OpenAPI contract                                        |
| `mapper`     | copy fields between two representations                                                              | the two types it maps                                                   | call a service or repository; do I/O                                     |

## Request flow

Laid out as a request flow, it reads almost like a sentence — a DTO becomes a model, a model
becomes an entity, and the response walks the same path back:

```
POST /api/users
  UserController(RegistrationRequest)
    → UserMapper.toModel(request)                   dto  → model
    → UserService.create(user)                      @Transactional
        → UserEntityMapper.toEntity(user)           model → entity
        → repository.save(entity)
        → UserEntityMapper.toModel(saved)           entity → model
    → UserMapper.toDto(user)                        model → response dto
```

A paged read follows the same idea: the controller hands a `Pageable` straight to the service,
the service returns a `Page<Model>`, and the controller wraps that into one list envelope
(`PagedResult` in `shared/`). See [`controllers.md`](controllers.md) for the code shape.

## Rules that hold it together

- The service is an interface plus one implementation. Everyone — controllers, other
  features — depends on the interface, never the impl directly.
- The repository stays a Spring Data interface. "Find or 404" is the service's job, not the
  repository's.
- `model/` never imports a framework type, and `entity/` never carries behaviour.
- A feature may call another feature's `service/` and use its `dto/` or `model/`, but it may
  never reach into another feature's `repository/` or `entity/` directly. That is the one hard
  boundary between features.
- Mappers are static and hold no state; they copy fields and nothing else.
- Controllers never call repositories, even for a simple lookup.

## Cross-feature calls

```
feed/service/FeedService
  → users/service/UserService       ✓ allowed
  → users/model/User                ✓ allowed
  → users/repository/UserRepository ✗ forbidden
  → users/entity/UserEntity         ✗ forbidden
```

If a feature needs data another feature owns, add or extend a method on that feature's service
interface. Do not join across repositories.

## Modules

Spring Modulith treats each direct subpackage of `dev.saswat.lifefeed` as a module. `shared` is
declared `OPEN`; other modules use the default (closed) rules. `ModularityTests` verifies the
structure, so a forbidden dependency fails the build. When adding a feature, add its package
under the base package rather than under an existing feature.

## Current state

`users` is still flat: there are no `controller/`, `service/`, `repository/`, `model/`,
`entity/`, `dto/` or `mapper/` subpackages yet, and `UserService` is a concrete class. The
target layout is the one above. Do not add new features to the flat package, and do not use
the current `users` package as an example of the finished structure.
