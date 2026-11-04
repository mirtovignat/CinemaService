package com.example.demo.model.entity;

import com.example.demo.model.base.AbstractCatalogItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rated_movies")
public class RatedMovie extends AbstractCatalogItem {

    @Column(name = "review", length = 350)
    private String review;

    @DecimalMin(value = "0.1", message = "Оценка не может быть меньше 0.1")
    @DecimalMax(value = "10.0", message = "Оценка не может быть больше 10")
    @Column(name = "rating_value", nullable = false, precision = 4, scale = 2)
    private BigDecimal ratingValue;

}