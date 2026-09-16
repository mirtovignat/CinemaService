package com.example.demo.repository.entity;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.Genre;
import com.example.demo.model.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {

    default Movie findByIdOrThrow(Long id) {
        return findById(id)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Query("""
            SELECT movie.rating
            FROM Movie movie
            WHERE movie.id = :id
            """)
    Optional<Double> findRatingById(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT movie.director
            FROM Movie movie
            WHERE movie.director IS NOT NULL
            ORDER BY movie.director
            """)
    List<String> findAllDistinctDirectors();

    @Query(value = "SELECT unnest(enum_range(NULL::genre_type))", nativeQuery = true)
    List<String> findAllGenresFromEnum();

    @Query("""
            SELECT DISTINCT movie
            FROM Movie movie
            LEFT JOIN movie.ratings ratedMovie
            WHERE movie.genre = :genre
            AND (ratedMovie IS NULL OR ratedMovie.deletableState.deleted = false)
            """)
    Page<Movie> findAllDistinctByGenre(Pageable pageable, @Param("genre") Genre genre);

    @Query("""
            SELECT DISTINCT movie
            FROM Movie movie
            LEFT JOIN movie.ratings ratedMovie
            WHERE (ratedMovie IS NULL OR ratedMovie.deletableState.deleted = false)
            """)
    Page<Movie> findAllWithActiveRatings(Pageable pageable);

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
            AND NOT EXISTS (
                SELECT 1 FROM RatedMovie ratedMovie
                WHERE ratedMovie.movie = movie
                AND ratedMovie.deletableState.deleted = true
            )
            ORDER BY movie.rating DESC NULLS LAST, movie.id ASC
            """)
    Slice<Movie> findRecommendedMoviesSlice(
            @Param("excludedIds") List<Long> excludedIds,
            Pageable pageable
    );
}