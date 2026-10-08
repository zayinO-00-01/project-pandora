package com.projectpandora.api.auth;

public record LoginResponse(String token, String role, String displayName, Long userId) {}
