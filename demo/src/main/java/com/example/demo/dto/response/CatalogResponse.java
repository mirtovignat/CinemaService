package com.example.demo.dto.response;

import com.example.demo.dto.user.UserForOwnerViewDTO;
import lombok.Builder;

import java.util.List;

@Builder
public record CatalogResponse<DTO>(
        UserForOwnerViewDTO userForOwner,
        List<DTO> catalog
) {
}
