package com.example.demo.dto.movie;

import com.example.demo.dto.base.*;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record MovieCardDetailsViewDTO(
        @JsonUnwrapped MovieIdentity id,
        @JsonUnwrapped MovieTitled title,
        @JsonUnwrapped MoviePricing price,
        @JsonUnwrapped MovieMedia media,
        @JsonUnwrapped MovieRating rating,
        @JsonUnwrapped MovieRelease release,
        @JsonUnwrapped MovieMeta meta
) {
}