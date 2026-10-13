package com.example.demo.dto.user;

import lombok.Builder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Builder
public record AppUserDetails(
        SessionUser sessionUser,
        String passwordHash
) implements UserDetails {

    public AppUserDetails {
        Objects.requireNonNull(sessionUser, "sessionUser must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return sessionUser.username();
    }

    public AppUserDetails withSessionUser(SessionUser updated) {
        return new AppUserDetails(updated, passwordHash);
    }
}