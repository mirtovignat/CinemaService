package com.example.demo.dto.base;

import com.example.demo.dto.catalog.ReviewViewDTO;
import lombok.Builder;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Builder
public record UserStatuses(
        Set<Long> bought,
        Set<Long> cart,
        Set<Long> favourites,
        Map<Long, ReviewViewDTO> rated
) {

    public UserStatuses {
        bought = bought == null ? Set.of() : new LinkedHashSet<>(bought);
        cart = cart == null ? Set.of() : new LinkedHashSet<>(cart);
        favourites = favourites == null ? Set.of() : new LinkedHashSet<>(favourites);
        rated = rated == null ? Map.of() : new LinkedHashMap<>(rated);
    }

    public static UserStatuses empty() {
        return UserStatuses.builder().build();
    }
}