package com.example.demo.dto.catalog;

import com.example.demo.dto.base.*;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record CartItemViewDTO(
        @JsonUnwrapped MovieIdentity id,
        @JsonUnwrapped MovieTitled title,
        @JsonUnwrapped MoviePricing price,
        @JsonUnwrapped MovieMedia media,
        @JsonUnwrapped MovieRating rating,
        @JsonUnwrapped MovieRelease release,
        @JsonUnwrapped MovieMeta meta,
        @JsonUnwrapped UserStatus status,
        @JsonUnwrapped CatalogQuantity quantity
) {
}