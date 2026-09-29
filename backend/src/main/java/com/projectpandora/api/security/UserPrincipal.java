package com.projectpandora.api.security;

import com.projectpandora.api.user.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final Role role;
    private final Long managerId;

    public UserPrincipal(Long id, String username, Role role, Long managerId) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.managerId = managerId;
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public Long getManagerId() {
        return managerId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
