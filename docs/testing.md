# Testing

JUnit 6, Mockito, AssertJ. Tests mirror the feature's own layer subpackages, and not every layer
needs one: a `service` test is **always** there, because that is where the business logic
actually lives.

## Which test for which layer

| Layer        | Test                                   | When                                                                    |
| ------------ | -------------------------------------- | ----------------------------------------------------------------------- |
| `service`    | Plain JUnit + Mockito                  | Always. Happy path, not-found, guard clauses, PATCH semantics.          |
| `controller` | `@WebMvcTest` or a full integration test | Once the endpoint has validation, status mapping or a response shape worth asserting. |
| `repository` | `@DataJpaTest`                         | Only for a custom `@Query` or `Specification` — never for plain CRUD.    |
| `mapper`     | Plain JUnit                            | When the mapper has non-trivial null/field handling.                    |

Spring Data already tested plain CRUD for you; a repository test that only calls `save` and
`findById` adds maintenance without adding confidence.

## Service unit tests

```java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTests {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void create_user_ok() {
        // given
        var command = new CreateUserDto("Ada", "ada@example.com", "secret");
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("encoded");

        // when
        var result = userService.create(command);

        // then
        assertAll(
                () -> assertThat(result.getEmail()).isEqualTo("ada@example.com"),
                () -> verify(userRepository).save(any(UserEntity.class)));
    }

    @Test
    void create_user_with_duplicate_email_ko() {
        // given
        var command = new CreateUserDto("Ada", "ada@example.com", "secret");
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(new UserEntity()));

        // when / then
        assertThatThrownBy(() -> userService.create(command))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorMessage())
                .isEqualTo(UserErrorMessage.USER_EMAIL_ALREADY_EXISTS);
        verify(userRepository, never()).save(any());
    }
}
```

Two details are easy to get backwards:

- In the service's own unit test, `@InjectMocks` targets the **impl** — Mockito cannot
  instantiate an interface.
- Everywhere else, `@Mock` / `@MockitoBean` the **interface** instead.

If `mockito-junit-jupiter` is not already on the test classpath, add it; Boot's test starters
usually bring it in.

## Controller tests

- `@WebMvcTest` is enough when you are asserting validation, status codes and the response
  shape. Disable the security filters (`@AutoConfigureMockMvc(addFilters = false)`) when the
  test is about the controller, not about authentication.
- Use a full integration test (extends `BaseIT`) when the test crosses security, JWT, the
  database or the error handler.
- Assert the `ProblemDetail` shape too: `title`, `status`, `code`, `errors`, `traceId`.

## Repository and integration tests

- The project runs against Testcontainers PostgreSQL 18 — there is no H2 profile.
- Integration tests extend `BaseIT`, which boots the app on a random port with
  `@ActiveProfiles("test")` and `@Import(TestcontainersConfig.class)`.
- Use `@Sql("/test-data.sql")` when a test needs the seeded users; the script deletes and
  re-inserts rows, so tests that share it must not depend on ordering.
- A `@DataJpaTest` that needs the container should reuse the same Testcontainers configuration;
  do not introduce a second database image.
- Never assert against generated IDs, timestamps or sequence values that the fixture does not
  control.

## Naming and shape

- Test names are `snake_case`, ending in `_ok` or `_ko`
  (`create_user_ok`, `get_user_not_found_ko`). A longer, more descriptive name is fine when it
  genuinely reads better.
- Every body follows `// given / // when / // then`.
- Assertions go through AssertJ (`assertThat`, `assertThatThrownBy`), and related checks get
  grouped into one `assertAll(...)` so a failure shows every mismatch at once instead of just
  the first.
- `var` for locals, no `final` — the same as controllers, and the opposite of the rest of
  production code.
- Field injection (`@Mock` / `@InjectMocks` / `@Autowired`) is fine here; it is only banned in
  production code.
- Keep reflection and business logic (loops, conditionals) out of a test body. If you need a
  helper, write a small private factory method.

## Coverage expectations

Per service, cover:

- the happy path;
- the not-found case;
- any guard clause that should reject before a save happens
  (`verify(repo, never()).save(any())`);
- PATCH/PUT semantics — a partial update really only touches the fields it was given.

JaCoCo enforces a 40% line-coverage minimum in `verify`. That is a floor, not a target; a
service with no test should not be merged.

## Quality gate

```shell
./mvnw spotless:check && ./mvnw test
```

`./mvnw clean verify` runs the full suite, Spotless and the JaCoCo check. CI runs
`./mvnw -ntp verify`.

## Current state

The existing tests (`UserRestControllerTests`, `AuthControllerTests`) are integration tests
against the running app. They are useful and should stay. Add service unit tests as the
`users` feature is refactored; do not replace the integration tests with mocks.
