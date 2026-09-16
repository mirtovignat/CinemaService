package com.example.demo.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class HistoryGrouper {

    public <T> Map<LocalDate, List<T>> groupByDateDesc(
            List<T> items,
            Function<T, LocalDateTime> dateExtractor
    ) {
        if (items == null || items.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<LocalDate, List<T>> grouped = items.stream()
                .collect(Collectors.groupingBy(
                        item -> dateExtractor.apply(item).toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, List<T>>comparingByKey().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .sorted(Comparator.comparing(dateExtractor).reversed())
                                .toList(),
                        (old, newVal) -> old,
                        LinkedHashMap::new
                ));
    }
}