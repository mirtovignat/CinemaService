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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {

    default Movie findByIdOrThrow(Long id) {
        return findById(id)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Query("""
            SELECT DISTINCT movie.director
            FROM Movie movie
            WHERE movie.director IS NOT NULL
            ORDER BY movie.director
            """)
    List<String> findAllDistinctDirectors();

    @Query("""
            SELECT DISTINCT movie.genre
            FROM Movie movie
            WHERE movie.genre IS NOT NULL
            ORDER BY movie.genre
            """)
    List<String> findAllDistinctGenres();

    @Query("""
            SELECT DISTINCT movie.director
            FROM Movie movie
            WHERE movie.director IS NOT NULL
            AND (:hasCursor = false OR movie.director > :cursor)
            AND (LOWER(movie.director) LIKE LOWER(CONCAT(:prefix, '%')))
            ORDER BY movie.director ASC
            """)
    Slice<String> fetchDirectorsKeyset(
            @Param("prefix") String prefix,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursor") String cursor,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT movie.genre
            FROM Movie movie
            WHERE movie.genre IS NOT NULL
            AND (:hasCursor = false OR movie.genre > :cursor)
            AND (LOWER(movie.genre) LIKE LOWER(CONCAT(:prefix, '%')))
            ORDER BY movie.genre ASC
            """)
    Slice<String> fetchGenresKeyset(
            @Param("prefix") String prefix,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursor") String cursor,
            Pageable pageable
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
            @Param("excludedIds") List<Long> excludedIds,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursorRating") BigDecimal cursorRating,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT movie.rating
            FROM Movie movie
            WHERE movie.id = :id
            """)
    Optional<Double> findRatingById(@Param("id") Long id);

    @Query("""
            SELECT movie
            FROM Movie movie
            """)
    Page<Movie> findAllPaged(Pageable pageable);

    @Query("""
            SELECT movie
            FROM Movie movie
            WHERE movie.posterUrl IS NULL
            ORDER BY movie.id ASC
            """)
    List<Movie> findByPosterUrlIsNull();

    @Query("""
            SELECT movie
            FROM Movie movie
            WHERE (:excludedIds IS NULL OR movie.id NOT IN :excludedIds)
            AND movie.title IS NOT NULL
            AND movie.title <> ''
            AND movie.posterUrl IS NOT NULL
            AND movie.posterUrl <> ''
            ORDER BY movie.rating DESC NULLS LAST, movie.id ASC
            """)
    Slice<Movie> findRecommendedMoviesSlice(
            @Param("excludedIds") List<Long> excludedIds,
            Pageable pageable
    );

    @Query("""
            SELECT movie
            FROM Movie movie
            WHERE movie.genre = :genre
            """)
    Page<Movie> findAllByGenre(Pageable pageable, @Param("genre") String genre);

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
}