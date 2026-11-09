Rather than a new exception class for every failure mode, keep one domain exception type that carries an error-code enum: `throw new AppException(AppErrorMessage.TASK_NOT_FOUND)`. Once the app grows, give each feature its own enum (`TaskErrorMessage`) behind a shared `ErrorMessage` interface, so a feature owns its codes without one giant enum that everybody edits.

New failure modes just mean a new enum value, not a new class to wire up.

Everything funnels through one `GlobalExceptionHandler` (`@ControllerAdvice` + `@ExceptionHandler`), which is the single place that maps exceptions to HTTP status codes and to a consistent error-response shape. Resist adding a local `@ExceptionHandler` in a controller — it just means there are now two places to check when a status code looks wrong.

For the response shape, use [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) (`application/problem+json`) instead of an invented `{ code, message }` object. Spring supports it natively (`ProblemDetail`), clients and API gateways already understand it, and it has a defined place for extra fields — which is where the trace ID from [`docs/observability.md`](#docsobservabilitymd) goes. Put the mapping in one class (`ProblemDetailMapper`) and reuse it everywhere an error body is written. That includes the errors that never reach your handler: Spring MVC's own 404/405/415 and the 401/403 raised by the security filter chain. If those paths build their own body, clients end up with two error formats.

One rule that's easy to miss: code that throws a domain exception doesn't also log it. The global handler logs once, in one place; logging at the throw site too just writes the same failure to your logs twice.
