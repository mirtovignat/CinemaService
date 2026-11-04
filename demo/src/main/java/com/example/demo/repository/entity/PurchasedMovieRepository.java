package com.example.demo.repository.entity;

import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchasedMovieRepository extends AbstractCatalogItemRepository<PurchasedMovie> {
}