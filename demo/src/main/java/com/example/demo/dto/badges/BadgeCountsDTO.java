package com.example.demo.dto.badges;

import lombok.Builder;

@Builder(toBuilder = true)
public record BadgeCountsDTO(
        Long cartCount,
        Long favouritesCount
) {
    public BadgeCountsDTO {
        cartCount = cartCount != null ? cartCount : 0L;
        favouritesCount = favouritesCount != null ? favouritesCount : 0L;
    }
}