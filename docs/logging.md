# Logging

Use a hand-written SLF4J logger — this project does not use Lombok:

```java
private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);
```

ArchUnit enforces the field name and modifiers (`private static final Logger LOG`). Do not
create a logger with a different name, and do not use `System.out`.

## Levels

- `WARN` / `ERROR` when something has gone wrong.
- `INFO` for actions worth a trail: a resource created, a job finished, a source synchronised.
- `DEBUG` for development detail; it is enabled for `dev.saswat.lifefeed` in
  `application.properties`.
- Do not log at `INFO` inside a loop or on every request unless the event is genuinely
  interesting.

## Message template

Keep the message template consistent across the codebase:

```
[<FEATURE>] - ACTION: <method>: <field>: {}
```

```java
LOG.info("[USERS] - CREATE: createUser: userId: {}", created.getId());
LOG.warn("[GITHUB] - SYNC: syncActivity: source: {} - rate limited", source);
LOG.error("[FEED] - READ: getFeed: userId: {} - failed", userId, exception);
```

- Use `{}` placeholders — never string concatenation. Concatenation is a performance cost and
  an easy way to leak something sensitive.
- Put the exception last and let SLF4J print the stack trace: `LOG.error("...", exception)`.
- Do not write request identifiers (trace ID, span ID, user ID) into the message by hand — the
  MDC adds them. See [`observability.md`](observability.md).

## Never log personal data

Logs are kept and shipped. Never log:

- email addresses, names, phone numbers, addresses;
- passwords, password hashes, JWTs, refresh tokens, API keys;
- request or response bodies that may contain any of the above.

The current `UserRestController` and `AuthController` log the registration/login email. That
is debt: remove those lines when you touch the files. Log the user ID (an opaque identifier),
not the email.

## Where to log

- Keep logging in the service / application layer.
- A thrown domain exception does not also log. The global exception handler logs once, in one
  place; logging at the throw site too writes the same failure to your logs twice.
- Controllers log at most an entry/exit line for an unusual action; they do not log request
  bodies.

## Correlation

`application.properties` already configures:

```properties
logging.pattern.correlation=[${spring.application.name:},%X{traceId:-},%X{spanId:-}]
```

Every line then carries the application name and, when a trace is active, the W3C trace and
span IDs. Add `%X{userId:-}` only when the MDC actually contains a verified user ID. Do not
invent a different correlation format.
