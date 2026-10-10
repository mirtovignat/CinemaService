package com.example.demo.dto.user;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record UserForOwnerViewDTO(
        Long id,
        BigDecimal balance,
        String currencyCode,
        String email,
        String username,
        String phone,
        String surname,
        String name,
        String patronymic,
        LocalDate birthDate,
        int age,
        boolean sex
) {}