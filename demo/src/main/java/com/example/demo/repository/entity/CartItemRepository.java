package com.example.demo.repository.entity;

import com.example.demo.model.entity.CartItem;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CartItemRepository extends AbstractCatalogItemRepository<CartItem> {

    @Query("""
            SELECT cartItem
            FROM CartItem cartItem
            JOIN FETCH cartItem.movie
            WHERE cartItem.id.userId = :userId
            AND (
                :hasCursor = false
                OR cartItem.addedAt > :cursorAddedAt
                OR (cartItem.addedAt = :cursorAddedAt AND cartItem.id.movieId > :cursorMovieId)
            )
            ORDER BY cartItem.addedAt ASC, cartItem.id.movieId ASC
            """)
    Slice<CartItem> fetchAscending(@Param("userId") Long userId,
                                   @Param("hasCursor") boolean hasCursor,
                                   @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                   @Param("cursorMovieId") Long cursorMovieId,
                                   Pageable pageable);

    @Query("""
            SELECT cartItem
            FROM CartItem cartItem
            JOIN FETCH cartItem.movie
            WHERE cartItem.id.userId = :userId
            AND (
                :hasCursor = false
                OR cartItem.addedAt < :cursorAddedAt
                OR (cartItem.addedAt = :cursorAddedAt AND cartItem.id.movieId < :cursorMovieId)
            )
            ORDER BY cartItem.addedAt DESC, cartItem.id.movieId DESC
            """)
    Slice<CartItem> fetchDescending(@Param("userId") Long userId,
                                    @Param("hasCursor") boolean hasCursor,
                                    @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                    @Param("cursorMovieId") Long cursorMovieId,
                                    Pageable pageable);
}