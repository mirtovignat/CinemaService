package com.example.demo.repository.entity;

import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchasedMovieRepository extends AbstractCatalogItemRepository<PurchasedMovie> {

    @Query("""
            SELECT item
            FROM PurchasedMovie item
            JOIN FETCH item.movie
            JOIN FETCH item.user
            WHERE item.id.movieId = :movieId
            AND item.id.userId = :userId
            """)
    Optional<PurchasedMovie> findByMovieIdAndUserId(@Param("movieId") Long movieId,
                                                    @Param("userId") Long userId);
}