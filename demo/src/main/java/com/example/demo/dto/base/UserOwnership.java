package com.example.demo.dto.base;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserOwnership(
        boolean own,
        LocalDateTime addedAt
) {
}