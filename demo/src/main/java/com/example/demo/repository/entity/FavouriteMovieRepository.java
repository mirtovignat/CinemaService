// demo/src/main/java/com/example/demo/repository/entity/FavouriteMovieRepository.java
package com.example.demo.repository.entity;

import com.example.demo.model.entity.FavouriteMovie;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FavouriteMovieRepository extends AbstractCatalogItemRepository<FavouriteMovie> {

    @Query("""
            SELECT item
            FROM FavouriteMovie item
            JOIN FETCH item.movie
            WHERE item.user.id = :userId
            AND (:cursorMovieId IS NULL OR item.id.movieId < :cursorMovieId)
            ORDER BY item.id.movieId DESC
            """)
    Slice<FavouriteMovie> findPageDesc(@Param("userId") Long userId,
                                       @Param("cursorMovieId") Long cursorMovieId,
                                       Pageable pageable);

    @Query("""
            SELECT item
            FROM FavouriteMovie item
            JOIN FETCH item.movie
            WHERE item.user.id = :userId
            AND (:cursorMovieId IS NULL OR item.id.movieId > :cursorMovieId)
            ORDER BY item.id.movieId ASC
            """)
    Slice<FavouriteMovie> findPageAsc(@Param("userId") Long userId,
                                      @Param("cursorMovieId") Long cursorMovieId,
                                      Pageable pageable);
}