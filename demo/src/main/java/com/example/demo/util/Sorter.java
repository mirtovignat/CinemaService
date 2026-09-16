package com.example.demo.util;

import com.example.demo.dto.catalog.ReviewViewDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Sorter {

    public static List<ReviewViewDTO> sortByReleaseDateConsideringOwn(List<ReviewViewDTO> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return reviews;
        }
        return reviews.stream()
                .sorted(Comparator
                        .comparing((ReviewViewDTO reviewViewDTO) -> reviewViewDTO.ownership().own(), Comparator.reverseOrder())
                        .thenComparing(dto -> dto.ownership().addedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }
}