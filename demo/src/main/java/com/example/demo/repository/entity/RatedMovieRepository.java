package com.example.demo.repository.entity;

import com.example.demo.model.entity.Movie;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.base.AbstractCatalogRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatedMovieRepository extends AbstractCatalogRepository<RatedMovie> {

    @Override
    @Query("""
            SELECT item
            FROM RatedMovie item
            JOIN FETCH item.user
            JOIN FETCH item.movie
            WHERE item.movie.id = :movieId
            AND item.deletableState.deleted = false
            """)
    List<RatedMovie> findAllByMovieId(@Param("movieId") Long movieId);

    @Override
    @Query("""
            SELECT item
            FROM RatedMovie item
            JOIN FETCH item.movie
            JOIN FETCH item.user
            WHERE item.user.id = :userId
            AND item.movie.id = :movieId
            AND item.deletableState.deleted = false
            """)
    Optional<RatedMovie> findByMovieIdAndUserId(@Param("movieId") Long movieId,
                                                @Param("userId") Long userId);

    @Query("""
        SELECT item
        FROM RatedMovie item
        JOIN FETCH item.movie
        JOIN FETCH item.user
        WHERE item.movie.id = :movieId
        AND item.user.id = :userId
        """)
    Optional<RatedMovie> findByMovieIdAndUserIdIncludingDeleted(@Param("movieId") Long movieId,
                                                                @Param("userId") Long userId);

    @Query("""
            SELECT COUNT(item)
            FROM RatedMovie item
            WHERE item.user.id = :userId
            AND item.deletableState.deleted = false
            """)
    Long countByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(ratedMovie)
            FROM RatedMovie ratedMovie
            WHERE ratedMovie.movie.id = :movieId
            AND ratedMovie.deletableState.deleted = false
            """)
    Long countByMovieId(@Param("movieId") Long movieId);

    @Override
    @Query("""
            SELECT COUNT(item) > 0
            FROM RatedMovie item
            WHERE item.movie.id = :movieId
            AND item.user.id = :userId
            AND item.deletableState.deleted = false
            """)
    boolean existsByMovieIdAndUserId(@Param("movieId") Long movieId,
                                     @Param("userId") Long userId);

    @Modifying
    @Query("""
            UPDATE RatedMovie ratedMovie
            SET ratedMovie.deletableState.deleted = true
            WHERE ratedMovie.id.movieId = :movieId
            AND ratedMovie.id.userId = :userId
            AND ratedMovie.deletableState.deleted = false
            """)
    int softDeleteByMovieIdAndUserId(@Param("movieId") Long movieId,
                                     @Param("userId") Long userId);

    @Query("""
            SELECT item
            FROM RatedMovie item
            WHERE item.user.id = :userId
            AND item.deletableState.deleted = false
            ORDER BY item.ratingValue DESC
            """)
    List<RatedMovie> findTopNByUserIdOrderByRatingValueDesc(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
            SELECT ratedMovie.movie.id
            FROM RatedMovie ratedMovie
            WHERE ratedMovie.user.id = :userId
            AND ratedMovie.deletableState.deleted = false
            """)
    List<Long> findMovieIdsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT AVG(ratedMovie.ratingValue)
            FROM RatedMovie ratedMovie
            WHERE ratedMovie.movie = :movie
            AND ratedMovie.deletableState.deleted = false
            """)
    BigDecimal calculateAverageRating(@Param("movie") Movie movie);
}