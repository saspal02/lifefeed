package com.saswat.lifefeed.users;

import java.time.Instant;

public record UserDto(Long id, String name, String email, Role role, Instant createdAt) {}
