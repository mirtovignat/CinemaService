package com.example.demo.dto.response;

import com.example.demo.dto.user.UserForOwnerViewDTO;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Builder
public record GroupedHistoryResponse<DTO>(
        UserForOwnerViewDTO userForOwner,
        Map<LocalDate, List<DTO>> groupedItems
) {
}