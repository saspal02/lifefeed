# Observability

## The problem

With several users at once, a log line like `[FEED] - READ: getFeed: userId: 42 - failed` does
not tell you which request produced it. The fix is a **request ID** that appears on every log
line of that request and in every error body, so a user can read it out and support can `grep`
it.

## Trace context, not an invented ID

Do not invent the ID format. Every request gets a [W3C Trace Context](https://www.w3.org/TR/trace-context/)
trace ID (32 hex characters), created by Micrometer Tracing with the OpenTelemetry bridge. If a
client sends the standard `traceparent` header, the backend continues that trace; otherwise it
starts one.

You do not need an exporter: using traces only for log correlation is a valid, cheap first
step.

Add the bridge when tracing is enabled:

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
```

`spring-boot-starter-actuator` is already present. Set a sampling probability for the local
profile (for example `management.tracing.sampling.probability=1.0`) and keep a lower value in
production.

## Rules

- **Do not put IDs into log messages by hand.** The MDC adds `[traceId,spanId,userId]` through
  `logging.pattern.correlation`; keep your own `[FEATURE] - ACTION: …` format.
- **Log IDs, never personal data.** `userId` is the only user field in the MDC; never an email,
  name, IBAN or token, because logs are kept and shipped.
- **Never trust a client header for a log field.** `userId` comes from the verified JWT
  principal only. Keep W3C baggage disabled so a `baggage: userId=…` header cannot spoof it.
- **`@Async` loses the MDC unless you copy it.** The worker thread is not the request thread, so
  configure one shared executor whose `TaskDecorator` copies the MDC and trace context, and
  reuse it for every new executor. Otherwise the follow-up work logs without IDs.
- **Outgoing HTTP clients use the injected `RestClient.Builder`**, never the static
  `RestClient.builder()`: only the injected one is instrumented, so the call gets a span and
  carries `traceparent`.
- **Every error path carries the `traceId`**, filters included (see
  [`exceptions.md`](exceptions.md)).

## Adding `userId` to the MDC

Populate it from the authenticated principal after the JWT filter has validated the token, and
always clear it:

```java
try {
    MDC.put("userId", String.valueOf(userId));
    chain.doFilter(request, response);
} finally {
    MDC.remove("userId");
}
```

Do not add `userId` to the MDC from a request header, query parameter or request body.

Then extend the pattern:

```properties
logging.pattern.correlation=[${spring.application.name:},%X{traceId:-},%X{spanId:-},%X{userId:-}]
```

## Async executor

```java
@Configuration
@EnableAsync
class AsyncConfig {
    @Bean
    TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            var context = MDC.getCopyOfContextMap();

            return () -> {
                if (context != null) {
                    MDC.setContextMap(context);
                }
                try {
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }
}
```

For trace-context propagation as well as MDC, Spring's
`ContextPropagatingTaskDecorator` is the supported abstraction; either use it or extend the
decorator above so it copies both. A decorator that copies only the MDC gives you log
correlation but not a child span.

Do not create a second executor for a new `@Async` method. Reuse the shared one.

## Outgoing HTTP

```java
// ✓ instrumented
class GithubClient {
    private final RestClient restClient;

    GithubClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.github.com").build();
    }
}
```

```java
// ✗ no span, no traceparent
RestClient client = RestClient.builder().baseUrl("https://api.github.com").build();
```

## Error bodies

`ProblemDetailMapper` adds `traceId` from the MDC. The security entry point and access-denied
handler use the same mapper, so a 401/403 also carries the ID a user can quote.

## Current state

The tracing dependency is not in `pom.xml` yet, and no `TaskDecorator` exists. The logging
pattern already has `traceId`/`spanId` placeholders, so adding the bridge is the main step.
Treat this page as the target when an integration or an `@Async` path is added.
