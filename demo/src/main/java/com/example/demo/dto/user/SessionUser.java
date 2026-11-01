package com.example.demo.dto.user;

import com.example.demo.model.entity.User;
import com.example.demo.util.text.StringSanitizer;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Locale;

@Builder(toBuilder = true)
public record SessionUser(
        Long id,
        BigDecimal balance,
        String currencyCode,
        String username
) {

    public static final SessionUser ANONYMOUS = SessionUser.builder().build();

    public boolean isAuthenticated() {
        return id != null;
    }

    public Long idOrNull() {
        return id;
    }

    public String getFirstUsernameLetter() {
        return StringSanitizer.trimToNull(username)
                .map(usernamePart -> usernamePart.substring(0, 1).toUpperCase(Locale.ROOT))
                .orElse("U");
    }

    public static SessionUser from(User user) {
        return SessionUser.builder()
                .id(user.getId())
                .balance(user.getBalance())
                .currencyCode(user.getCurrencyCode())
                .username(user.getUsername())
                .build();
    }

    public SessionUser withBalance(BigDecimal newBalance) {
        return this.toBuilder()
                .balance(newBalance)
                .build();
    }

    public SessionUser withUsername(String newUsername) {
        return this.toBuilder()
                .username(newUsername)
                .build();
    }
}