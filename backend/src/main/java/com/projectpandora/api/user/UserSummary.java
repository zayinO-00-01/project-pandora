package com.projectpandora.api.user;

public record UserSummary(Long id, String username, String displayName, Role role) {
    public static UserSummary from(UserEntity e) {
        return new UserSummary(e.getId(),e.getUsername(),e.getDisplayName(),e.getRole());
    }
}
