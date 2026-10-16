// demo/src/main/java/com/example/demo/repository/entity/CartItemRepository.java
package com.example.demo.repository.entity;

import com.example.demo.model.entity.CartItem;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends AbstractCatalogItemRepository<CartItem> {

    @Query("""
            SELECT item
            FROM CartItem item
            JOIN FETCH item.movie
            WHERE item.user.id = :userId
            AND (:cursorMovieId IS NULL OR item.id.movieId < :cursorMovieId)
            ORDER BY item.id.movieId DESC
            """)
    Slice<CartItem> findPageDesc(@Param("userId") Long userId,
                                 @Param("cursorMovieId") Long cursorMovieId,
                                 Pageable pageable);

    @Query("""
            SELECT item
            FROM CartItem item
            JOIN FETCH item.movie
            WHERE item.user.id = :userId
            AND (:cursorMovieId IS NULL OR item.id.movieId > :cursorMovieId)
            ORDER BY item.id.movieId ASC
            """)
    Slice<CartItem> findPageAsc(@Param("userId") Long userId,
                                @Param("cursorMovieId") Long cursorMovieId,
                                Pageable pageable);
}