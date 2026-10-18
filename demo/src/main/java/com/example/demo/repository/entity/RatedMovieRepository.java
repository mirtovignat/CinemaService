package com.example.demo.repository.entity;

import com.example.demo.model.entity.Movie;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatedMovieRepository extends AbstractCatalogItemRepository<RatedMovie> {

    @Override
    @Query("""
            SELECT ratedMovie FROM RatedMovie ratedMovie
            JOIN FETCH ratedMovie.movie
            WHERE ratedMovie.id.userId = :userId
            AND ratedMovie.deletableState.deleted = false
            AND (
                :hasCursor = false
                OR ratedMovie.addedAt > :cursorAddedAt
                OR (ratedMovie.addedAt = :cursorAddedAt AND ratedMovie.id.movieId > :cursorMovieId)
            )
            ORDER BY ratedMovie.addedAt ASC, ratedMovie.id.movieId ASC
            """)
    Slice<RatedMovie> fetchAscending(@Param("userId") Long userId,
                                     @Param("hasCursor") boolean hasCursor,
                                     @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                     @Param("cursorMovieId") Long cursorMovieId,
                                     Pageable pageable);

    @Override
    @Query("""
            SELECT ratedMovie FROM RatedMovie ratedMovie
            JOIN FETCH ratedMovie.movie
            WHERE ratedMovie.id.userId = :userId
            AND ratedMovie.deletableState.deleted = false
            AND (
                :hasCursor = false
                OR ratedMovie.addedAt < :cursorAddedAt
                OR (ratedMovie.addedAt = :cursorAddedAt AND ratedMovie.id.movieId < :cursorMovieId)
            )
            ORDER BY ratedMovie.addedAt DESC, ratedMovie.id.movieId DESC
            """)
    Slice<RatedMovie> fetchDescending(@Param("userId") Long userId,
                                      @Param("hasCursor") boolean hasCursor,
                                      @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                      @Param("cursorMovieId") Long cursorMovieId,
                                      Pageable pageable);

    @Query("""
            SELECT ratedMovie FROM RatedMovie ratedMovie
            JOIN FETCH ratedMovie.user
            WHERE ratedMovie.id.movieId = :movieId
            AND ratedMovie.deletableState.deleted = false
            AND (:viewerUserId IS NULL OR ratedMovie.id.userId <> :viewerUserId)
            AND (
                :hasCursor = false
                OR ratedMovie.addedAt > :cursorAddedAt
                OR (ratedMovie.addedAt = :cursorAddedAt AND ratedMovie.id.userId > :cursorUserId)
            )
            ORDER BY ratedMovie.addedAt ASC, ratedMovie.id.userId ASC
            """)
    Slice<RatedMovie> fetchReviewsAscending(@Param("movieId") Long movieId,
                                            @Param("viewerUserId") Long viewerUserId,
                                            @Param("hasCursor") boolean hasCursor,
                                            @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                            @Param("cursorUserId") Long cursorUserId,
                                            Pageable pageable);

    @Query("""
            SELECT ratedMovie FROM RatedMovie ratedMovie
            JOIN FETCH ratedMovie.user
            WHERE ratedMovie.id.movieId = :movieId
            AND ratedMovie.deletableState.deleted = false
            AND (:viewerUserId IS NULL OR ratedMovie.id.userId <> :viewerUserId)
            AND (
                :hasCursor = false
                OR ratedMovie.addedAt < :cursorAddedAt
                OR (ratedMovie.addedAt = :cursorAddedAt AND ratedMovie.id.userId < :cursorUserId)
            )
            ORDER BY ratedMovie.addedAt DESC, ratedMovie.id.userId DESC
            """)
    Slice<RatedMovie> fetchReviewsDescending(@Param("movieId") Long movieId,
                                             @Param("viewerUserId") Long viewerUserId,
                                             @Param("hasCursor") boolean hasCursor,
                                             @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                             @Param("cursorUserId") Long cursorUserId,
                                             Pageable pageable);

    @Override
    @Query("""
            SELECT DISTINCT ratedMovie FROM RatedMovie ratedMovie
            JOIN FETCH ratedMovie.movie
            JOIN FETCH ratedMovie.user
            WHERE ratedMovie.id.userId = :userId
            AND ratedMovie.deletableState.deleted = false
            """)
    List<RatedMovie> findAllByUserId(@Param("userId") Long userId);

    @Override
    @Query("""
            SELECT COUNT(ratedMovie) > 0 FROM RatedMovie ratedMovie
            WHERE ratedMovie.id.movieId = :movieId
            AND ratedMovie.id.userId = :userId
            AND ratedMovie.deletableState.deleted = false
            """)
    boolean existsByMovieIdAndUserId(@Param("movieId") Long movieId,
                                     @Param("userId") Long userId);

    @Override
    @Query("""
            SELECT COUNT(ratedMovie) FROM RatedMovie ratedMovie
            WHERE ratedMovie.id.userId = :userId
            AND ratedMovie.deletableState.deleted = false
            """)
    Long countByUserId(@Param("userId") Long userId);

    @Override
    @Query("""
            SELECT ratedMovie.id.movieId FROM RatedMovie ratedMovie
            WHERE ratedMovie.id.userId = :userId
            AND ratedMovie.deletableState.deleted = false
            """)
    List<Long> findMovieIdsByUserId(@Param("userId") Long userId);

    @Override
    @Query("""
            SELECT ratedMovie FROM RatedMovie ratedMovie
            JOIN FETCH ratedMovie.movie
            JOIN FETCH ratedMovie.user
            WHERE ratedMovie.id.movieId = :movieId
            AND ratedMovie.id.userId = :userId
            AND ratedMovie.deletableState.deleted = false
            """)
    Optional<RatedMovie> findByMovieIdAndUserId(@Param("movieId") Long movieId,
                                                @Param("userId") Long userId);

    @Query("""
            SELECT ratedMovie FROM RatedMovie ratedMovie
            JOIN FETCH ratedMovie.movie
            JOIN FETCH ratedMovie.user
            WHERE ratedMovie.id.movieId = :movieId
            AND ratedMovie.id.userId = :userId
            """)
    Optional<RatedMovie> findByMovieIdAndUserIdIncludingDeleted(
            @Param("movieId") Long movieId,
            @Param("userId") Long userId);

    @Query("""
            SELECT ratedMovie FROM RatedMovie ratedMovie
            JOIN FETCH ratedMovie.movie
            JOIN FETCH ratedMovie.user
            WHERE ratedMovie.id.movieId = :movieId
            AND ratedMovie.id.userId = :userId
            AND ratedMovie.deletableState.deleted = false
            """)
    Optional<RatedMovie> findOwnReview(@Param("movieId") Long movieId,
                                       @Param("userId") Long userId);

    @Query("""
            SELECT COUNT(ratedMovie) FROM RatedMovie ratedMovie
            WHERE ratedMovie.id.movieId = :movieId
            AND ratedMovie.deletableState.deleted = false
            """)
    Long countByMovieId(@Param("movieId") Long movieId);

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

    @Modifying
    @Query("""
            UPDATE RatedMovie ratedMovie
            SET ratedMovie.deletableState.deleted = true
            WHERE ratedMovie.id.userId = :userId
            AND ratedMovie.deletableState.deleted = false
            """)
    void softDeleteAllByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT ratedMovie FROM RatedMovie ratedMovie
            WHERE ratedMovie.id.userId = :userId
            AND ratedMovie.deletableState.deleted = false
            ORDER BY ratedMovie.ratingValue DESC
            """)
    List<RatedMovie> findTopNByUserIdOrderByRatingValueDesc(
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("""
            SELECT AVG(ratedMovie.ratingValue) FROM RatedMovie ratedMovie
            WHERE ratedMovie.movie = :movie
            AND ratedMovie.deletableState.deleted = false
            """)
    BigDecimal calculateAverageRating(@Param("movie") Movie movie);

    @Query("""
            SELECT ratedMovie FROM RatedMovie ratedMovie
            JOIN FETCH ratedMovie.movie
            JOIN FETCH ratedMovie.user
            WHERE ratedMovie.id.userId = :userId
            AND ratedMovie.deletableState.deleted = false
            ORDER BY ratedMovie.addedAt DESC, ratedMovie.id.userId DESC
            """)
    List<RatedMovie> findAllActiveByUserId(@Param("userId") Long userId);
}