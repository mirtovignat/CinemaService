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
        Objects.requireNonNull(sessionUser, "sessionUser не должен быть равен null");
        Objects.requireNonNull(passwordHash, "passwordHash не должен быть равен null");
        if (!sessionUser.isAuthenticated()) {
            throw new IllegalArgumentException("AppUserDetails не может быть создан для неаутентифицированного пользователя");
        }
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

    public AppUserDetails withSessionUser(SessionUser updatedSessionUser) {
        return AppUserDetails.builder()
                .sessionUser(updatedSessionUser)
                .passwordHash(getPassword())
                .build();
    }
}