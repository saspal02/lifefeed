# Mappers

Pick one approach as a team and stick with it — mixing the two per feature is where mapper code
turns into archaeology.

**This project uses static mappers.** There is no MapStruct dependency and no generated mapper
code. MapStruct remains a valid alternative if mapping becomes non-trivial; it is described at
the bottom so the choice is explicit rather than accidental.

## Two mappers per feature

| Class                    | Converts       | Used by    |
| ------------------------ | -------------- | ---------- |
| `<Feature>Mapper`        | DTO ↔ `model`  | controller |
| `<Feature>EntityMapper`  | `model` ↔ `entity` | service |

The split keeps the controller from knowing about JPA and the service from knowing about the
wire format.

## Rules

- `public final class` with a private constructor that throws:
  `throw new UnsupportedOperationException("This class should never be instantiated");`.
- Name the static methods for the direction they map: `toModel`, `toEntity`, `toDto`,
  `fromCreateDto`, `fromUpdateDto`.
- Guard against `null` at the top of every method and return `null` for a `null` input.
- Build into a local variable before returning it rather than passing a builder chain straight
  into another call.
- A mapper's whole job is copying fields — no I/O, no calling a service or repository, no
  validation.
- DTOs are records; models and entities use hand-written accessors.

## Example

```java
public final class UserMapper {
    private UserMapper() {
        throw new UnsupportedOperationException("This class should never be instantiated");
    }

    public static User toModel(CreateUserDto dto) {
        if (dto == null) {
            return null;
        }

        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());

        return user;
    }

    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        UserDto dto = new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());

        return dto;
    }
}
```

```java
public final class UserEntityMapper {
    private UserEntityMapper() {
        throw new UnsupportedOperationException("This class should never be instantiated");
    }

    public static UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }

        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());
        entity.setRole(user.getRole());

        return entity;
    }

    public static User toModel(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        User user = new User();
        user.setId(entity.getId());
        user.setName(entity.getName());
        user.setEmail(entity.getEmail());
        user.setRole(entity.getRole());
        user.setCreatedAt(entity.getCreatedAt());

        return user;
    }
}
```

## MapStruct alternative

If the team later adopts MapStruct:

- Define mapper interfaces with `@Mapper`, use `@Mapping` for fields that do not line up by
  name.
- Set `componentModel = "spring"` so Spring manages the generated instance.
- Suffix the interface `Mapper` and keep method names direction-obvious (`toDto`, `toEntity`).
- In tests, `Mappers.getMapper(UserMapper.class)` gets an instance without a Spring context.

Do not mix static mappers and MapStruct in the same feature.

## Current state

`users` has no mapper classes yet; `UserService` and the controllers map inline. When the
feature is refactored into layers, extract `UserMapper` and `UserEntityMapper` following this
page. Do not create a mapper for a single trivial field copy that the service can do once.
