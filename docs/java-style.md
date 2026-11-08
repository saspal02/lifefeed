# Java Style

Most of this is boring on purpose. Formatting should not be a matter of taste: pick a rule, wire
it into the build, and stop arguing about it.

## Formatting

- 4-space indentation, 120-character lines, UTF-8, IntelliJ IDEA's default Java style.
- Enforce it with Spotless + [Palantir Java Format](https://github.com/palantir/palantir-java-format),
  already configured in `pom.xml`:
  - `./mvnw spotless:apply` rewrites the files.
  - `./mvnw spotless:check` fails the build when someone forgot.
- Spotless runs during `compile`; CI runs `./mvnw -ntp verify`, so a formatting miss fails the
  build.
- Blank lines separate logical blocks. Put a blank line before and after a `return`, a `for`
  loop, or a stream chain — unless it is the first or last line in its block.

## Modifiers and types

- **No `final` on method parameters or local variables, anywhere.** It survives only on
  constructor-injected fields (`private final UserRepository userRepository;`), because that
  field is part of the constructor contract.
- **`var` in controllers and tests; explicit types in services, mappers and the rest of
  production code.** Controllers and tests are short and local; services and mappers are read
  by people debugging behaviour, where the type is part of the explanation.
- Prefer immutability, especially inside `for`-each loops and `Stream.forEach()`.
- No magic numbers or strings — name them in a constant or an enum.

## Methods and parameters

- No more than 3 parameters on a method or constructor. If you need more, the related ones
  usually belong in a record.
- Check nullness and emptiness before you touch a collection or a string.
- Skip `throws` clauses in favour of unchecked exceptions.
- Use `@Override` whenever you override something.
- Skip comments except for cron expressions, regular expressions, `TODO`s, and
  given/when/then separators in tests.
- No wildcard imports and no Javadoc on internal code.
- For one or two variables, a plain `== null` reads better than `Objects.isNull()`.
- Wrap compound conditions in a named boolean instead of inlining them.
- Prefer early returns over nested `if`/`else`.

## Null and emptiness

```java
if (email == null || email.isBlank()) {
    throw new AppException(UserErrorMessage.EMAIL_REQUIRED);
}

if (users == null || users.isEmpty()) {
    return List.of();
}
```

Do not return `null` collections. A repository query that can find nothing should return
`Optional`; the service turns that into a domain exception or a list.

## Control flow

```java
// Prefer this
if (user == null) {
    throw new AppException(UserErrorMessage.USER_NOT_FOUND);
}

return UserMapper.toDto(user);

// Over this
if (user != null) {
    return UserMapper.toDto(user);
} else {
    throw new AppException(UserErrorMessage.USER_NOT_FOUND);
}
```

## Naming

- Packages: lowercase, no underscores.
- Classes and records: `PascalCase`.
- Methods and fields: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- Booleans read as predicates: `isActive`, `hasNextPage`, `canRetry`.
- ArchUnit/Taikai enforces the layer suffixes:
  - `*Controller` for web controllers
  - `*Service` for services (interface and implementation)
  - `*Repository` for Spring Data repositories
  - `*Config` for `@Configuration` classes
  - `private static final Logger LOG` for loggers

## Imports

- Explicit imports only; Spotless removes unused ones and sorts them.
- Static imports are fine for AssertJ (`assertThat`, `assertThatThrownBy`) and for a small set
  of HTTP statuses where the code reads better.
- Avoid static wildcard imports. The existing `import static org.springframework.http.HttpStatus.*`
  in `GlobalExceptionHandler` is debt; replace it with explicit names when touching the file.
- Never import `jakarta.persistence.*` into `model/`, and never import a controller from a
  service.

## Existing debt

The current code predates a few of these rules. When you touch a file, align it:

- `FlywayConfig` declares a `final` local variable.
- `SecurityUser` and several configuration classes still use explicit constructor bodies; that
  is acceptable, but keep fields `final`.
- `GlobalExceptionHandler` uses a static wildcard import.
- Controllers and services still contain a few explicit local types where `var` would be used
  in a controller or test.

Do not reformat unrelated files in the same change.
