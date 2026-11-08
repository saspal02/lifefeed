# Controllers

Think of `<Feature>Controller` as the HTTP edge and nothing more: it maps the request DTO to a
model, calls the service, maps the result back to a response DTO. No business logic, no
existence checks, no `@ExceptionHandler` living locally — see [`exceptions.md`](exceptions.md)
for where that goes instead.

## The one habit that matters

Every mapping is its own statement, assigned to a `var`, and the service call is its own
statement too. Never nest a mapper call inside the service call, and never nest the service call
inside a mapper call.

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public RegistrationResponse createUser(@RequestBody @Valid RegistrationRequest request) {
    var user = UserMapper.toModel(request);
    var created = userService.create(user);

    return UserMapper.toDto(created);
}
```

Not this — it saves two lines and costs you a debugger breakpoint the day something in here
throws:

```java
// ✗ nested mapping — request-map buried in the argument list, response-map wrapping the call
return UserMapper.toDto(userService.create(UserMapper.toModel(request)));
```

## Parameters

- One or two filter parameters are fine as individual `@RequestParam`s.
- Three or more (dates, enums, and so on) deserve one object bound with `@ModelAttribute`
  instead of a long parameter list.
- Paged search endpoints take a `Pageable` and return the list envelope. Name the handler and
  its service method `search…`, never `list…`, even when there is no filter to speak of.

```java
@GetMapping
public PagedResult<UserDto> searchUsers(Pageable pageable) {
    var page = userService.search(pageable);
    var result = PagedResult.from(page.map(UserMapper::toDto));

    return result;
}
```

`PagedResult` lives in `shared/` and produces `{ data, currentPageNo, totalPages,
totalElements, hasNextPage, hasPreviousPage }`.

## Status codes

- `CREATED` on create (`@ResponseStatus(HttpStatus.CREATED)` or `ResponseEntity` when the
  location header is dynamic).
- `NO_CONTENT` on delete.
- Everything else defaults to 200.
- Prefer `@ResponseStatus` for a fixed status. Use `ResponseEntity` only when the status or
  headers vary at runtime.

## Validation

- `@RequestBody @Valid` on every write DTO.
- Validation lives on the DTO record with Jakarta Bean Validation annotations; the controller
  does not re-check fields.
- A validation failure is handled by `GlobalExceptionHandler` and returns a `ProblemDetail`
  with an `errors` list.

## Before you commit a controller method

- `var` locals only — no `final`, no explicit types.
- Request-map → service call → response-map are separate lines, each into a `var`.
- No mapper call nested in a service call or another mapper call.
- No `findById(...).isPresent()` / existence checks — throw from the service instead.
- No `@ExceptionHandler` in the controller.
- No repository or entity import.
- The method calls exactly one service method.

## Current state

`UserRestController` and `AuthController` still declare their request/response records as nested
types, map inline, and return `ResponseEntity` for fixed statuses. That is debt. New endpoints
put DTOs in `dto/`, use the static mappers, and follow the shape above. Do not add another nested
record to a controller.
