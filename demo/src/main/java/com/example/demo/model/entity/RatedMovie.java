package com.example.demo.model.entity;

import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.model.base.DeletableState;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rated_movies",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "user_id", "movie_id"
        }))
public class RatedMovie extends AbstractCatalogItem {

    @Embedded
    @Builder.Default
    private DeletableState deletableState = DeletableState.builder().build();

    @Column(name = "review")
    private String review;

    @DecimalMin(value = "0.1", message = "Оценка не может быть меньше 0.1")
    @DecimalMax(value = "10.0", message = "Оценка не может быть больше 10")
    @Column(name = "rating_value", nullable = false, precision = 4, scale = 2)
    private BigDecimal ratingValue;

}