package com.example.demo.repository.entity;

import com.example.demo.model.entity.FavouriteMovie;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface FavouriteMovieRepository extends AbstractCatalogItemRepository<FavouriteMovie> {

    @Query("""
            SELECT favouriteMovie
            FROM FavouriteMovie favouriteMovie
            JOIN FETCH favouriteMovie.movie
            WHERE favouriteMovie.id.userId = :userId
            AND (
                :hasCursor = false
                OR favouriteMovie.addedAt > :cursorAddedAt
                OR (favouriteMovie.addedAt = :cursorAddedAt AND favouriteMovie.id.movieId > :cursorMovieId)
            )
            ORDER BY favouriteMovie.addedAt ASC, favouriteMovie.id.movieId ASC
            """)
    Slice<FavouriteMovie> fetchAscending(@Param("userId") Long userId,
                                         @Param("hasCursor") boolean hasCursor,
                                         @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                         @Param("cursorMovieId") Long cursorMovieId,
                                         Pageable pageable);

    @Query("""
            SELECT favouriteMovie
            FROM FavouriteMovie favouriteMovie
            JOIN FETCH favouriteMovie.movie
            WHERE favouriteMovie.id.userId = :userId
            AND (
                :hasCursor = false
                OR favouriteMovie.addedAt < :cursorAddedAt
                OR (favouriteMovie.addedAt = :cursorAddedAt AND favouriteMovie.id.movieId < :cursorMovieId)
            )
            ORDER BY favouriteMovie.addedAt DESC, favouriteMovie.id.movieId DESC
            """)
    Slice<FavouriteMovie> fetchDescending(@Param("userId") Long userId,
                                          @Param("hasCursor") boolean hasCursor,
                                          @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                          @Param("cursorMovieId") Long cursorMovieId,
                                          Pageable pageable);
}