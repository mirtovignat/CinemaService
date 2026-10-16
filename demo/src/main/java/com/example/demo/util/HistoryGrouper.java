package com.example.demo.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HistoryGrouper {

    public static <T> Map<LocalDate, List<T>> groupByDateDesc(
            List<T> items,
            Function<T, LocalDateTime> dateExtractor
    ) {
        if (items == null || items.isEmpty()) {
            return new LinkedHashMap<>();
        }

        return items.stream()
                .sorted(Comparator.comparing(dateExtractor).reversed())
                .collect(Collectors.groupingBy(
                        item -> dateExtractor.apply(item).toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }
}