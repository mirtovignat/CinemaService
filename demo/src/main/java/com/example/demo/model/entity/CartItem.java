package com.example.demo.model.entity;

import com.example.demo.model.base.AbstractCatalogItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "movie_id"}))
public class CartItem extends AbstractCatalogItem {

    @Builder.Default
    @Column(name = "quantity", nullable = false)
    private Short quantity = 1;

}