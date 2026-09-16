package com.example.demo.dto.user;

import com.example.demo.model.entity.User;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record SessionUser(
        Long id,
        BigDecimal balance,
        String currencyCode,
        String username
) {
    public String getFirstUsernameLetter() {
        String username = username();
        if (username == null || username.isBlank()) {
            return "U";
        }
        String firstChar = username.substring(0, username.offsetByCodePoints(0, 1));
        return firstChar.toUpperCase(java.util.Locale.ROOT);
    }

    public static SessionUser empty() {
        return SessionUser.builder()
                .id(null)
                .balance(BigDecimal.ZERO)
                .currencyCode("₽")
                .username("username")
                .build();
    }

    public static SessionUser from(User user) {
        if (user == null) {
            return empty();
        }
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

    public SessionUser withUsername(String username) {
        return SessionUser.builder()
                .id(this.id)
                .balance(this.balance)
                .currencyCode(this.currencyCode)
                .username(username)
                .build();
    }
}