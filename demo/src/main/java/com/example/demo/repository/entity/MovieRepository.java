package com.example.demo.repository.entity;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.projection.MovieRangeProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {

    default Movie findByIdOrThrow(Long id) {
        return findById(id)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Modifying
    @Query("""
            UPDATE Movie movie
            SET movie.ratingSum = movie.ratingSum + :deltaSum,
                movie.ratingCount = movie.ratingCount + :deltaCount,
                movie.rating = CASE
                    WHEN (movie.ratingCount + :deltaCount) > 0
                        THEN ROUND((movie.ratingSum + :deltaSum) / (movie.ratingCount + :deltaCount), 2)
                    ELSE NULL
                END
            WHERE movie.id = :movieId
            """)
    void applyRatingDelta(@Param("movieId") Long movieId,
                         @Param("deltaSum") BigDecimal deltaSum,
                         @Param("deltaCount") long deltaCount);

    @Query("""
            SELECT movie.rating
            FROM Movie movie
            WHERE movie.id = :id
            """)
    Optional<BigDecimal> findRatingById(@Param("id") Long id);

    @Query("""
            SELECT
                MIN(movie.duration)    AS minDuration,
                MAX(movie.duration)    AS maxDuration,
                MIN(movie.price)       AS minPrice,
                MAX(movie.price)       AS maxPrice,
                MIN(movie.releaseDate) AS minReleaseDate,
                MAX(movie.releaseDate) AS maxReleaseDate
            FROM Movie movie
            """)
    MovieRangeProjection findRanges();

    @Query("""
            SELECT DISTINCT movie.genre
            FROM Movie movie
            ORDER BY movie.genre ASC
            """)
    List<String> findAllDistinctGenres();

    @Query(value = """
            SELECT director
            FROM movies
            WHERE director IS NOT NULL
              AND LOWER(TRANSLATE(director, 'ёЁ', 'еЕ')) LIKE LOWER(CONCAT(:searchQuery, '%')) ESCAPE '\\'
              AND (
                  :hasCursor = false
                  OR LOWER(TRANSLATE(director, 'ёЁ', 'еЕ')) > LOWER(TRANSLATE(:cursor, 'ёЁ', 'еЕ'))
              )
            GROUP BY director
            ORDER BY LOWER(TRANSLATE(director, 'ёЁ', 'еЕ')) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<String> fetchDirectorsKeyset(
            @Param("searchQuery") String searchQuery,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursor") String cursor,
            @Param("limit") int limit
    );

    @Query("""
            SELECT movie
            FROM Movie movie
            WHERE (:excludedIds IS NULL OR movie.id NOT IN :excludedIds)
            AND movie.title IS NOT NULL
            AND movie.title <> ''
            AND movie.posterUrl IS NOT NULL
            AND movie.posterUrl <> ''
            AND (
                :hasCursor = false
                OR movie.rating < :cursorRating
                OR (movie.rating = :cursorRating AND movie.id > :cursorId)
                OR (movie.rating IS NULL AND :cursorRating IS NOT NULL)
                OR (movie.rating IS NULL AND :cursorRating IS NULL AND movie.id > :cursorId)
            )
            ORDER BY movie.rating DESC NULLS LAST, movie.id ASC
            """)
    Slice<Movie> findRecommendedMoviesSliceKeyset(
            @Param("excludedIds") Collection<Long> excludedIds,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursorRating") BigDecimal cursorRating,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query(value = """
            SELECT genre
            FROM movies
            WHERE LOWER(TRANSLATE(genre, 'ёЁ', 'еЕ')) LIKE LOWER(CONCAT(:searchQuery, '%')) ESCAPE '\\'
              AND (:hasCursor = false OR LOWER(TRANSLATE(genre, 'ёЁ', 'еЕ')) > LOWER(TRANSLATE(:cursor, 'ёЁ', 'еЕ')))
            GROUP BY genre
            ORDER BY LOWER(TRANSLATE(genre, 'ёЁ', 'еЕ')) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<String> fetchGenresKeyset(
            @Param("searchQuery") String searchQuery,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursor") String cursor,
            @Param("limit") int limit
    );

    Page<Movie> findByGenre(String genre, Pageable pageable);

    @Query(value = """
        SELECT DISTINCT movie_id FROM (
            SELECT movie_id FROM cart_items WHERE user_id = :userId
            UNION ALL
            SELECT movie_id FROM favourite_movies WHERE user_id = :userId
            UNION ALL
            SELECT movie_id FROM purchased_movies WHERE user_id = :userId
            UNION ALL
            SELECT movie_id FROM rated_movies WHERE user_id = :userId
        ) AS all_ids
        """, nativeQuery = true)
    List<Long> findExcludedMovieIdsByUserId(@Param("userId") Long userId);
}