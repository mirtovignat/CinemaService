package com.example.demo.dto.base;

import lombok.Builder;

@Builder
public record MovieIdentity(
        Long id
) {
}