package com.example.demo.repository.entity;

import com.example.demo.model.entity.CartItem;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import com.example.demo.repository.projection.CartSummaryProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends AbstractCatalogItemRepository<CartItem> {

    @Query("""
            SELECT COUNT(cartItem) AS count,
                   COALESCE(SUM(cartItem.movie.price), 0) AS total
            FROM CartItem cartItem
            WHERE cartItem.abstractCatalogItemId.userId = :userId
            AND NOT EXISTS (
                SELECT 1 FROM PurchasedMovie purchasedMovie
                WHERE purchasedMovie.abstractCatalogItemId.userId = :userId
                AND purchasedMovie.abstractCatalogItemId.movieId = cartItem.abstractCatalogItemId.movieId
            )
            """)
    CartSummaryProjection findPurchasableSummaryByUserId(@Param("userId")
                                                         Long userId);
}