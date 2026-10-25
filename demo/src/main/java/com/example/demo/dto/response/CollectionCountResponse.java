package com.example.demo.dto.response;

import lombok.Builder;

@Builder
public record CollectionCountResponse(
        Long count
) {
}