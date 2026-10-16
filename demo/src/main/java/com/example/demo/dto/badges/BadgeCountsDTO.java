package com.example.demo.dto.badges;

import lombok.Builder;

@Builder(toBuilder = true)
public record BadgeCountsDTO(
        Long cartCount,
        Long favouritesCount,
        Long purchasesCount,
        Long ratingsCount
) {
    public BadgeCountsDTO {
        cartCount = cartCount != null ? cartCount : 0L;
        favouritesCount = favouritesCount != null ? favouritesCount : 0L;
        purchasesCount = purchasesCount != null ? purchasesCount : 0L;
        ratingsCount = ratingsCount != null ? ratingsCount : 0L;
    }
}