package com.example.demo.model.entity;

import com.example.demo.model.base.AbstractCatalogItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "cart_items")
public class CartItem extends AbstractCatalogItem {

    @Builder.Default
    @Column(name = "quantity", nullable = false)
    private Short quantity = 1;

}