# Exceptions

Rather than a new exception class for every failure mode, keep one domain exception type that
carries an error-code enum:

```java
throw new AppException(UserErrorMessage.USER_NOT_FOUND);
```

Once the app grows, give each feature its own enum (`UserErrorMessage`, `FeedErrorMessage`,
`GithubErrorMessage`) behind a shared `ErrorMessage` interface, so a feature owns its codes
without one giant enum that everybody edits.

New failure modes mean a new enum value, not a new class to wire up.

## The error-code contract

```java
public interface ErrorMessage {
    HttpStatus status();
    String code();
    String title();
    String detail();
}
```

```java
public enum UserErrorMessage implements ErrorMessage {
    USER_NOT_FOUND(NOT_FOUND, "user-not-found", "User not found", "The requested user does not exist"),
    USER_EMAIL_ALREADY_EXISTS(CONFLICT, "user-email-exists", "Email already registered", "That email is already in use");

    private final HttpStatus status;
    private final String code;
    private final String title;
    private final String detail;

    UserErrorMessage(HttpStatus status, String code, String title, String detail) {
        this.status = status;
        this.code = code;
        this.title = title;
        this.detail = detail;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String detail() {
        return detail;
    }
}
```

`AppException` carries the enum plus an optional override detail:

```java
public class AppException extends RuntimeException {
    private final ErrorMessage errorMessage;

    public AppException(ErrorMessage errorMessage) {
        super(errorMessage.detail());
        this.errorMessage = errorMessage;
    }

    public AppException(ErrorMessage errorMessage, String detail) {
        super(detail);
        this.errorMessage = errorMessage;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }
}
```

## One handler

Everything funnels through one `GlobalExceptionHandler`
(`@RestControllerAdvice` + `@ExceptionHandler`), which is the single place that maps exceptions
to HTTP status codes and to a consistent error-response shape. Resist adding a local
`@ExceptionHandler` in a controller — it just means there are now two places to check when a
status code looks wrong.

The handler covers at least:

- `AppException` → its `ErrorMessage` status.
- `MethodArgumentNotValidException` → 400 with an `errors` list.
- `BadCredentialsException` → 401.
- `AccessDeniedException` → 403.
- `DataIntegrityViolationException` → 409 (the database is the last line of defence; the service
  should have checked first).
- `Exception` → 500 with a generic detail, never a stack trace or internal message.

## Problem Details

Use [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457)
(`application/problem+json`) instead of an invented `{ code, message }` object. Spring supports
it natively (`ProblemDetail`), clients and API gateways already understand it, and it has a
defined place for extra fields — which is where the trace ID from
[`observability.md`](observability.md) goes.

Put the mapping in one class (`ProblemDetailMapper`) and reuse it everywhere an error body is
written. That includes the errors that never reach your handler: Spring MVC's own 404/405/415
and the 401/403 raised by the security filter chain. If those paths build their own body, clients
end up with two error formats.

```java
public final class ProblemDetailMapper {
    private ProblemDetailMapper() {
        throw new UnsupportedOperationException("This class should never be instantiated");
    }

    public static ProblemDetail toProblemDetail(ErrorMessage errorMessage, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorMessage.status(), detail);
        problem.setTitle(errorMessage.title());
        problem.setProperty("code", errorMessage.code());
        problem.setProperty("traceId", MDC.get("traceId"));

        return problem;
    }
}
```

`traceId` may be `null` when tracing is not active (unit tests, CLI runs). Serialize it as
absent or null, not as the literal string `"null"`.

Example body:

```json
{
  "type": "about:blank",
  "title": "User not found",
  "status": 404,
  "detail": "The requested user does not exist",
  "instance": "/api/users/42",
  "code": "user-not-found",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
}
```

## Security filter errors

The 401 from the authentication entry point and the 403 from the access-denied handler must use
the same `ProblemDetailMapper`, with `application/problem+json`. Do not let the security chain
return an empty body or Spring's default HTML/JSON error page.

## Logging rule

Code that throws a domain exception does **not** also log it. The global handler logs once, in
one place; logging at the throw site too just writes the same failure to your logs twice. See
[`logging.md`](logging.md).

## Current state

The code has `UserNotFoundException`, `ResourceNotFoundException` and `BadRequestException`, and
`GlobalExceptionHandler` returns `ProblemDetail` for most of them. Consolidate these into
`AppException` + a feature `ErrorMessage` enum when the feature is refactored. Until then, new
failure modes go through `AppException`, not a new exception class.
