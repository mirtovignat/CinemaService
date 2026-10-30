package com.example.demo.repository.entity;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.projection.MovieRangeProjection;
import com.example.demo.repository.projection.MovieReferProjection;
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
import java.util.Set;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {

    default Movie findByIdOrThrow(Long id) {
        return findById(id)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ENTITY_NOT_FOUND));
    }

    boolean existsByTmdbId(Long tmdbId);

    @Query("""
            SELECT movie.tmdbId
            FROM Movie movie
            WHERE movie.tmdbId IN :tmdbIds
            """)
    Set<Long> findExistingTmdbIds(@Param("tmdbIds") Collection<Long> tmdbIds);

    @Query("""
            SELECT movie.id            AS id,
                   movie.title         AS title,
                   movie.releaseDate   AS releaseDate,
                   movie.tmdbId        AS tmdbId
            FROM Movie movie
            WHERE movie.tmdbId IN :tmdbIds
            """)
    List<MovieReferProjection> findRefsByTmdbIds(@Param("tmdbIds") Collection<Long> tmdbIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Movie movie
            SET movie.posterUrl = :url
            WHERE movie.id = :id
            """)
    int updatePosterUrl(@Param("id") Long id,
                        @Param("url") String url);

    @Query(value = """
            SELECT DISTINCT INITCAP(g)
            FROM movies, unnest(genre) AS g
            WHERE g IS NOT NULL
            ORDER BY INITCAP(g)
            """, nativeQuery = true)
    List<String> findAllDistinctGenres();

    @Query(value = """
            SELECT director
            FROM movies
            WHERE director IS NOT NULL
              AND LOWER(director) LIKE LOWER(CONCAT(:prefix, '%'))
              AND (
                  :hasCursor = false
                  OR LOWER(director) > LOWER(:cursor)
              )
            GROUP BY director
            ORDER BY LOWER(director) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<String> fetchDirectorsKeyset(
            @Param("prefix") String prefix,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursor") String cursor,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT DISTINCT INITCAP(g)
            FROM movies, unnest(genre) AS g
            WHERE g IS NOT NULL
              AND (:hasCursor = false OR INITCAP(g) > :cursor)
              AND LOWER(g) LIKE LOWER(CONCAT(:prefix, '%'))
            ORDER BY INITCAP(g) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<String> fetchGenresKeyset(
            @Param("prefix") String prefix,
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

    @Query(value = """
            SELECT * FROM movies
            WHERE :genre = ANY(genre)
            """,
            countQuery = """
                    SELECT COUNT(*) FROM movies
                    WHERE :genre = ANY(genre)
                    """,
            nativeQuery = true)
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