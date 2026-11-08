# Security

The application protects a personal activity API. Treat every endpoint as private unless it is
explicitly listed as public.

## Authentication

- JWT access tokens and refresh tokens are signed with HMAC-SHA (jjwt).
- Access-token lifetime comes from `app.jwt.expires-in`; the refresh token is set in a
  `refresh_token` cookie.
- The refresh cookie must be `HttpOnly`, `Secure`, `Path=/`, and have a bounded `Max-Age`. It
  must not be readable from JavaScript.
- Passwords are hashed with BCrypt; never log or return them.
- `SecurityUser` is the principal. Derive the current user from
  `SecurityContextHolder` (`AuthUtils`), never from a request parameter or header.

## Endpoint access

Target rule: **authenticated by default**, with a short, explicit allow-list:

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/users` (registration)
- `/actuator/health/**`, `/actuator/info/**`
- `/swagger-ui/**`, `/v3/api-docs/**`, `/error`
- CORS pre-flight `OPTIONS`

Everything else requires a valid access token. Use `@PreAuthorize` at the controller layer for
role checks, and keep the URL rules and the method rules consistent.

### Current deviation

`WebSecurityConfig` currently ends with `.anyRequest().permitAll()`, so most endpoints are
reachable without a token, and `/api/auth/me` does its own 401 check in the controller. This is
known debt. New endpoints must not rely on it, and the chain should be tightened in a dedicated
change with integration tests for 401/403.

## Error responses

The authentication entry point (401) and the access-denied handler (403) must return the same
RFC 9457 `ProblemDetail` shape as the rest of the API, with `application/problem+json` and the
trace ID. Do not return an empty body, HTML or a different JSON shape from the filter chain.
See [`exceptions.md`](exceptions.md).

## Secrets

- Never commit real secrets. `app.jwt.secret` has a development default in
  `application.properties`; it must be overridden with an environment variable in every
  non-local environment.
- Read secrets from environment variables or a secret manager, not from a checked-in profile.
- Do not log secrets, tokens or password hashes.

## CORS

CORS is configured through `app.cors.*` in `WebMvcConfig`. Do not combine wildcard origins with
credentials. If the API is consumed by a browser app, list the exact origins.

## Input and output

- `@RequestBody @Valid` on every write DTO; validate length, format and enums.
- Do not bind an entity directly from a request; use a DTO and map explicitly.
- Do not expose an entity from a controller; map to a response DTO.
- Let Spring Data bind JPQL/SQL parameters; never concatenate user input into a query.
- File uploads are not part of the API yet. If they are added, validate type, size and storage
  path before writing anything.

## Logging and PII

Never log email addresses, names, passwords, tokens or request bodies that may contain them.
The current login/registration email logs are debt. Log the user ID instead. See
[`logging.md`](logging.md).

## Dependencies

Renovate keeps dependencies current. Review security-relevant upgrades (Spring Security, jjwt,
PostgreSQL driver) promptly; do not pin a version with a known CVE.

## When a security review is required

A change that touches authentication, authorization, JWT handling, cookies, uploads, SQL,
security headers or secrets gets a security review before merge. See
[`agentic-workflows.md`](agentic-workflows.md).
