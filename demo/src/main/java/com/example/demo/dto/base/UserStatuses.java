package com.example.demo.dto.base;

import lombok.Builder;

import java.util.Set;

@Builder
public record UserStatuses(
        Set<Long> bought,
        Set<Long> cart,
        Set<Long> favourites
) {
}