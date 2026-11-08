# Annotations

This project does **not** use Lombok. Keep the boilerplate explicit:

- DTOs are `record`s.
- Entities and models use hand-written getters/setters.
- Loggers are hand-written `private static final Logger LOG = LoggerFactory.getLogger(...)`.

If Lombok is ever adopted, it is a project-wide decision with its own change and review; do not
introduce it file by file.

## Spring stereotypes

- **`@RestController`** on web controllers, with the HTTP-method annotations (`@GetMapping` /
  `@PostMapping` / `@PutMapping` / `@PatchMapping` / `@DeleteMapping`) at method level.
- **`@Service`** on the feature's one implementation, `<Feature>ServiceImpl`, which
  `implements <Feature>Service` — a plain interface carrying no annotations of its own.
- **`@Repository`** is never written by hand: repositories are Spring Data interfaces
  (`extends JpaRepository<…>`) and Spring registers them for you. No `*RepositoryImpl`, no
  adapters.
- **`@Component`** for generic Spring beans, **`@Configuration`** for configuration classes.
- **`@Autowired`** means constructor injection in production code; field injection is reserved
  for tests.
- **`@ConfigurationProperties`** once you are binding three or more related properties — below
  that, individual `@Value`s are fine. This project already uses a single
  `ApplicationProperties` record with nested records.
- **`@Transactional`** lives at class level, on `@Service` classes only, so transaction
  management does not leak into individual methods. Use `@Transactional(readOnly = true)` for
  read paths or method-level overrides.
- **`@Validated`** to turn on Bean Validation on method parameters or classes;
  `@RequestBody @Valid` on write DTOs.
- **`@PreAuthorize`** at the controller layer when Spring Security is enforcing method-level
  access.
- Keep dependencies acyclic, and do not reach for `@Order` to paper over a resolution problem —
  fix the dependency instead.

## JPA

- `entity/` classes carry `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`,
  `@Enumerated` and the auditing annotations inherited from `BaseEntity`.
- `entity/` classes carry **no behaviour**: no validation logic, no calculations, no calls to
  services.
- `BaseEntity` is a `@MappedSuperclass` with `@EntityListeners(AuditingEntityListener.class)`,
  `@CreatedDate`, `@LastModifiedDate` and `@Version`.
- `model/` must not import `jakarta.persistence.*`.
- Prefer `EnumType.STRING` over the default ordinal.
- Use `@Column(nullable = false)` and database constraints together; the entity annotation is
  documentation, the migration is the enforcement.

## Web layer

- Class-level `@RequestMapping("/api/<feature>")`; method-level HTTP annotations.
- Prefer `@ResponseStatus(HttpStatus.CREATED)` for a fixed status. Use `ResponseEntity` only
  when the status or headers vary at runtime.
- One or two filter parameters are fine as individual `@RequestParam`s; three or more deserve
  one object bound with `@ModelAttribute`.
- Paged search endpoints take a `Pageable` and return the list envelope
  (`PagedResult` in `shared/`).
- `@RequestBody @Valid` on every write DTO.
- One `@RestControllerAdvice` (`GlobalExceptionHandler`) for the whole application. Do not add
  a local `@ExceptionHandler` in a controller.

## Async and scheduling

- `@Async` methods must run on an executor whose `TaskDecorator` copies the MDC and trace
  context. See [`observability.md`](observability.md).
- `@Scheduled` is for periodic source synchronisation; keep the cron expression in a named
  constant and document the timezone.

## What not to annotate

- Do not put `@Transactional` on controllers or repositories.
- Do not put `@Service` on an interface.
- Do not add `@Repository` to a Spring Data interface.
- Do not use `@Data`-style “all or nothing” generation. There is no Lombok; write the accessors
  you actually need.
