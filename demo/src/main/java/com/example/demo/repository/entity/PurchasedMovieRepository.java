package com.example.demo.repository.entity;

import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PurchasedMovieRepository extends AbstractCatalogItemRepository<PurchasedMovie> {

    @Override
    @Query(value = """
            SELECT DISTINCT purchasedMovie FROM PurchasedMovie purchasedMovie
            LEFT JOIN FETCH purchasedMovie.watchedHistory
            JOIN FETCH purchasedMovie.movie movie
            WHERE purchasedMovie.id.userId = :userId
            AND (
                :query IS NULL
                OR TRANSLATE(LOWER(movie.title), 'ё', 'е') LIKE CONCAT(CAST(:query AS string), '%') ESCAPE '\\'
            )
            AND (
                :hasCursor = false
                OR purchasedMovie.addedAt > :cursorAddedAt
                OR (purchasedMovie.addedAt = :cursorAddedAt AND purchasedMovie.id.movieId > :cursorMovieId)
            )
            ORDER BY purchasedMovie.addedAt ASC, purchasedMovie.id.movieId ASC
            """,
            countQuery = """
                    SELECT COUNT(purchasedMovie) FROM PurchasedMovie purchasedMovie
                    JOIN purchasedMovie.movie movie
                    WHERE purchasedMovie.id.userId = :userId
                    AND (
                        :query IS NULL
                        OR TRANSLATE(LOWER(movie.title), 'ё', 'е') LIKE CONCAT(CAST(:query AS string), '%') ESCAPE '\\'
                    )
                    """)
    Slice<PurchasedMovie> fetchAscending(@Param("userId") Long userId,
                                         @Param("hasCursor") boolean hasCursor,
                                         @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                         @Param("cursorMovieId") Long cursorMovieId,
                                         @Param("query") String query,
                                         Pageable pageable);

    @Override
    @Query(value = """
            SELECT DISTINCT purchasedMovie FROM PurchasedMovie purchasedMovie
            LEFT JOIN FETCH purchasedMovie.watchedHistory
            JOIN FETCH purchasedMovie.movie movie
            WHERE purchasedMovie.id.userId = :userId
            AND (
                :query IS NULL
                OR TRANSLATE(LOWER(movie.title), 'ё', 'е') LIKE CONCAT(CAST(:query AS string), '%') ESCAPE '\\'
            )
            AND (
                :hasCursor = false
                OR purchasedMovie.addedAt < :cursorAddedAt
                OR (purchasedMovie.addedAt = :cursorAddedAt AND purchasedMovie.id.movieId < :cursorMovieId)
            )
            ORDER BY purchasedMovie.addedAt DESC, purchasedMovie.id.movieId DESC
            """,
            countQuery = """
                    SELECT COUNT(purchasedMovie) FROM PurchasedMovie purchasedMovie
                    JOIN purchasedMovie.movie movie
                    WHERE purchasedMovie.id.userId = :userId
                    AND (
                        :query IS NULL
                        OR TRANSLATE(LOWER(movie.title), 'ё', 'е') LIKE CONCAT(CAST(:query AS string), '%') ESCAPE '\\'
                    )
                    """)
    Slice<PurchasedMovie> fetchDescending(@Param("userId") Long userId,
                                          @Param("hasCursor") boolean hasCursor,
                                          @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                          @Param("cursorMovieId") Long cursorMovieId,
                                          @Param("query") String query,
                                          Pageable pageable);
}