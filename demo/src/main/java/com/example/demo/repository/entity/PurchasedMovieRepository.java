package com.example.demo.repository.entity;

import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.repository.base.AbstractCatalogRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchasedMovieRepository extends AbstractCatalogRepository<PurchasedMovie> {

    @Override
    @Query("""
            SELECT COUNT(purchasedMovie) > 0
            FROM PurchasedMovie purchasedMovie
            WHERE purchasedMovie.movie.id = :movieId
            AND purchasedMovie.user.id = :userId
            """)
    boolean existsByMovieIdAndUserId(@Param("movieId") Long movieId,
                                     @Param("userId") Long userId);

    @Query("""
            SELECT purchasedMovie.movie.id
            FROM PurchasedMovie purchasedMovie
            WHERE purchasedMovie.user.id = :userId
            """)
    List<Long> findMovieIdsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(item)
            FROM PurchasedMovie item
            WHERE item.user.id = :userId
            """)
    Long countByUserId(@Param("userId") Long userId);
}