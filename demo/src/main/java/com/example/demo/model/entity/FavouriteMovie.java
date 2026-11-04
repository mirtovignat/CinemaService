package com.example.demo.model.entity;

import com.example.demo.model.base.AbstractCatalogItem;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "favourite_movies")
public class FavouriteMovie extends AbstractCatalogItem {
}