package dev.saswat.lifefeed.users;

record CreateUserCmd(String name, String email, String password, Role role) {}
