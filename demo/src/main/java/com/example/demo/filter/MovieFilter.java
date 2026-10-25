package com.example.demo.filter;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record MovieFilter(
        @PositiveOrZero(message = "Цена от не может быть отрицательной")
        BigDecimal priceFrom,

        @PositiveOrZero(message = "Цена до не может быть отрицательной")
        BigDecimal priceTo,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate releaseDateFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate releaseDateTo,

        @Min(value = 1, message = "Длительность от должна быть не менее 1 секунды")
        Long durationFrom,

        @Min(value = 1, message = "Длительность до должна быть не менее 1 секунды")
        Long durationTo,

        @DecimalMin(value = "0.1", message = "Рейтинг от должен быть не менее 0.1")
        @DecimalMax(value = "10.0", message = "Рейтинг от должен быть не более 10.0")
        BigDecimal ratingFrom,

        @DecimalMin(value = "0.1", message = "Рейтинг до должен быть не менее 0.1")
        @DecimalMax(value = "10.0", message = "Рейтинг до должен быть не более 10.0")
        BigDecimal ratingTo,

        List<String> genres,
        List<String> directors
) {

    public boolean hasAnyCriteria() {
        return (priceFrom != null) ||
                (priceTo != null) ||
                (releaseDateFrom != null) ||
                (releaseDateTo != null) ||
                (durationFrom != null) ||
                (durationTo != null) ||
                (ratingFrom != null) ||
                (ratingTo != null) ||
                (genres != null && !genres.isEmpty()) ||
                (directors != null && !directors.isEmpty());
    }
}