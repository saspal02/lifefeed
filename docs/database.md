# Database

PostgreSQL 18, Spring Data JPA and Flyway. Flyway owns the schema; Hibernate only validates it.

## Flyway

- Migrations live in `src/main/resources/db/migration`.
- Name them `V00N__snake_case_description.sql` (`V001__create_users_table.sql`,
  `V002__add_activity_events.sql`). Keep the zero-padded, monotonically increasing number.
- **Never edit an applied migration.** Flyway records a checksum; editing an applied file
  breaks every environment that already ran it. Add the next migration instead.
- Do not add a migration that depends on data inserted by a later migration.
- Destructive migrations — dropping a column or table, changing a type, rewriting a large table
  — require a human decision and a backup plan. Stop and ask before writing one.
- Keep a migration transactional unless it contains a statement PostgreSQL cannot run in a
  transaction (for example `CREATE INDEX CONCURRENTLY`); call that out in a comment.

## Hibernate

`application.properties` already sets:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
```

- Never change `ddl-auto` to `update`, `create` or `create-drop`, not even locally. The local
  profile may let Flyway clean and re-migrate (`FlywayConfig`), but it does not let Hibernate
  create the schema.
- `validate` means the entity mapping and the migration must agree. Add the column in the
  migration and the field in the entity in the same change.

## Tables

- Use `bigint` primary keys backed by a sequence
  (`create sequence user_id_seq start with 100 increment by 50;`). Do not use a natural key as
  the primary key.
- `created_at` is `not null default CURRENT_TIMESTAMP`; `updated_at` is nullable; `version` is
  `not null default 0` for optimistic locking.
- Store timestamps in UTC. The JDBC time zone is already set to UTC; do not store local time.
- Add `unique` constraints for business keys (the user email), `not null` for required columns,
  and foreign keys with an explicit `on delete` policy.
- Add indexes for the columns a query filters or sorts by; do not index everything.
- Store source-specific activity payloads in a `jsonb` column, not a growing set of nullable
  columns. The common event columns (`id`, `source`, `type`, `timestamp`, `summary`) stay
  relational.

## Entities and auditing

`BaseEntity` is a `@MappedSuperclass` with `@CreatedDate`, `@LastModifiedDate` and `@Version`.
Do not set `createdAt` by hand in a service when auditing can do it; the current
`UserService.createUser` does, which is debt. Let `AuditingEntityListener` fill it.

Entities are persistence rows, not domain models: no behaviour, no service calls, no DTO
imports. See [`layered-architecture.md`](layered-architecture.md).

## Queries

- Prefer derived query methods (`findByEmailIgnoreCase`) and `Pageable`/`Sort`.
- Use `@Query` only when a derived query cannot express the read; keep it in the repository
  interface, not in a hand-written implementation.
- Use `@Modifying` only for a deliberate bulk update; otherwise load the entity and let dirty
  checking write it inside the transaction. The current `UserRepository.updateUser` bulk
  update is debt for exactly that reason.
- Do not build JPQL or SQL with string concatenation. Bind parameters.
- Watch N+1 queries: use a fetch join or `@EntityGraph` when a response reads a collection.

## Tests

- Testcontainers runs PostgreSQL 18 (`postgres:18-alpine`); never substitute H2.
- `BaseIT` boots the app with the container; `@Sql("/test-data.sql")` seeds the fixture for
  integration tests.
- A repository test is justified only for a custom query or `Specification`. Plain CRUD is
  already covered by Spring Data.

## Local profile

`FlywayConfig` is `@Profile("local")` only. It retries a failed migration by cleaning the
database and migrating again. Never enable clean-and-migrate outside a disposable local
database.

## Current schema

`V001__create_users_table.sql` creates `user_id_seq`, the `users` table and two seeded users.
New features add `V002__…`, `V003__…`, and so on. Do not modify `V001`.
