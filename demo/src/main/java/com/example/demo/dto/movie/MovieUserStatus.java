package com.example.demo.dto.movie;

import com.example.demo.dto.catalog.ReviewViewDTO;
import lombok.Builder;

@Builder
public record MovieUserStatus(
        boolean bought,
        boolean inCart,
        boolean inFavourites,
        ReviewViewDTO ownRating
) {}