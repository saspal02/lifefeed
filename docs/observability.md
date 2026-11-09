The problem: with several users at once, a log line like `[LEDGER] - POST: unbalanced entry` doesn't tell you which request produced it. The fix is a **request ID** that appears on every log line of that request and in every error body, so a user can read it out and support can `grep` it.

Don't invent the ID format. Every request gets a [W3C Trace Context](https://www.w3.org/TR/trace-context/) trace ID (32 hex characters), created by Micrometer Tracing with the OpenTelemetry bridge. If a client sends the standard `traceparent` header, the backend continues that trace; otherwise it starts one. You don't need an exporter: using traces only for log correlation is a valid, cheap first step.

- **Don't put IDs into log messages by hand.** The MDC (the per-thread logging context) adds `[traceId,spanId,userId]` through `logging.pattern.correlation`; keep your own `[FEATURE] - ACTION: …` format.

- **Log IDs, never personal data.** `userId` is the only user field in the MDC; never an email, name, IBAN or token, because logs are kept and shipped.

- **Never trust a client header for a log field.** `userId` comes from the verified JWT principal only, and W3C baggage stays disabled so a `baggage: userId=…` header can't spoof it.

- **`@Async` loses the MDC unless you copy it.** The worker thread isn't the request thread, so configure one shared executor whose `TaskDecorator` copies the MDC and trace context, and reuse it for every new executor. Otherwise the follow-up work logs without IDs.

- **Outgoing HTTP clients use the injected `RestClient.Builder`**, never the static `RestClient.builder()`: only the injected one is instrumented, so the call gets a span and carries `traceparent`.

- **Every error path carries the `traceId`**, filters included (see [`docs/exceptions.md`](#docsexceptionsmd)).
