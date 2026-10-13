package com.example.demo.dto.user;

import com.example.demo.model.entity.User;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Locale;

@Builder
public record SessionUser(
        Long id,
        BigDecimal balance,
        String currencyCode,
        String username
) {
    public String getFirstUsernameLetter() {
        if (username == null || username.isBlank()) {
            return "U";
        }
        return username.substring(0, 1).toUpperCase(Locale.ROOT);
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
        return SessionUser.builder()
                .id(this.id)
                .balance(newBalance)
                .currencyCode(this.currencyCode)
                .username(this.username)
                .build();
    }

    public SessionUser withUsername(String newUsername) {
        return SessionUser.builder()
                .id(this.id)
                .balance(this.balance)
                .currencyCode(this.currencyCode)
                .username(newUsername)
                .build();
    }
}