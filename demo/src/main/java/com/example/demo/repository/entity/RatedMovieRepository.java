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
            SELECT DISTINCT item
            FROM RatedMovie item
            JOIN FETCH item.movie
            JOIN FETCH item.user
            WHERE item.user.id = :userId
            AND item.deletableState.deleted = false
            """)
    List<RatedMovie> findAllByUserId(@Param("userId") Long userId);

    @Override
    @Query("""
            SELECT COUNT(item) > 0
            FROM RatedMovie item
            WHERE item.id.movieId = :movieId
            AND item.id.userId = :userId
            AND item.deletableState.deleted = false
            """)
    boolean existsByMovieIdAndUserId(@Param("movieId") Long movieId,
                                     @Param("userId") Long userId);

    @Override
    @Query("""
            SELECT COUNT(item)
            FROM RatedMovie item
            WHERE item.id.userId = :userId
            AND item.deletableState.deleted = false
            """)
    Long countByUserId(@Param("userId") Long userId);

    @Override
    @Query("""
            SELECT item.id.movieId
            FROM RatedMovie item
            WHERE item.id.userId = :userId
            AND item.deletableState.deleted = false
            """)
    List<Long> findMovieIdsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT item
            FROM RatedMovie item
            JOIN FETCH item.movie
            JOIN FETCH item.user
            WHERE item.id.movieId = :movieId
            AND item.id.userId = :userId
            AND item.deletableState.deleted = false
            """)
    Optional<RatedMovie> findByMovieIdAndUserId(@Param("movieId") Long movieId,
                                                @Param("userId") Long userId);

    @Query("""
            SELECT item
            FROM RatedMovie item
            JOIN FETCH item.movie
            JOIN FETCH item.user
            WHERE item.id.movieId = :movieId
            AND item.id.userId = :userId
            """)
    Optional<RatedMovie> findByMovieIdAndUserIdIncludingDeleted(
            @Param("movieId") Long movieId,
            @Param("userId") Long userId);

    @Query("""
            SELECT item
            FROM RatedMovie item
            JOIN FETCH item.movie
            JOIN FETCH item.user
            WHERE item.id.movieId = :movieId
            AND item.id.userId = :userId
            AND item.deletableState.deleted = false
            """)
    Optional<RatedMovie> findOwnReview(@Param("movieId") Long movieId,
                                       @Param("userId") Long userId);

    @Query("""
            SELECT item
            FROM RatedMovie item
            JOIN FETCH item.user
            WHERE item.id.movieId = :movieId
            AND item.deletableState.deleted = false
            AND item.id.userId <> :viewerUserId
            ORDER BY item.addedAt DESC, item.id.userId DESC
            """)
    Slice<RatedMovie> findReviewsDescFirst(
            @Param("movieId") Long movieId,
            @Param("viewerUserId") Long viewerUserId,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM RatedMovie item
            JOIN FETCH item.user
            WHERE item.id.movieId = :movieId
            AND item.deletableState.deleted = false
            AND item.id.userId <> :viewerUserId
            AND (
                item.addedAt < :cursorAddedAt
                OR (item.addedAt = :cursorAddedAt AND item.id.userId < :cursorUserId)
            )
            ORDER BY item.addedAt DESC, item.id.userId DESC
            """)
    Slice<RatedMovie> findReviewsDescAfter(
            @Param("movieId") Long movieId,
            @Param("viewerUserId") Long viewerUserId,
            @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
            @Param("cursorUserId") Long cursorUserId,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM RatedMovie item
            JOIN FETCH item.user
            WHERE item.id.movieId = :movieId
            AND item.deletableState.deleted = false
            AND item.id.userId <> :viewerUserId
            ORDER BY item.addedAt ASC, item.id.userId ASC
            """)
    Slice<RatedMovie> findReviewsAscFirst(
            @Param("movieId") Long movieId,
            @Param("viewerUserId") Long viewerUserId,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM RatedMovie item
            JOIN FETCH item.user
            WHERE item.id.movieId = :movieId
            AND item.deletableState.deleted = false
            AND item.id.userId <> :viewerUserId
            AND (
                item.addedAt > :cursorAddedAt
                OR (item.addedAt = :cursorAddedAt AND item.id.userId > :cursorUserId)
            )
            ORDER BY item.addedAt ASC, item.id.userId ASC
            """)
    Slice<RatedMovie> findReviewsAscAfter(
            @Param("movieId") Long movieId,
            @Param("viewerUserId") Long viewerUserId,
            @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
            @Param("cursorUserId") Long cursorUserId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(ratedMovie)
            FROM RatedMovie ratedMovie
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

    @Query("""
            SELECT item
            FROM RatedMovie item
            WHERE item.id.userId = :userId
            AND item.deletableState.deleted = false
            ORDER BY item.ratingValue DESC
            """)
    List<RatedMovie> findTopNByUserIdOrderByRatingValueDesc(
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("""
            SELECT AVG(ratedMovie.ratingValue)
            FROM RatedMovie ratedMovie
            WHERE ratedMovie.movie = :movie
            AND ratedMovie.deletableState.deleted = false
            """)
    BigDecimal calculateAverageRating(@Param("movie") Movie movie);

    @Query("""
            SELECT item
            FROM RatedMovie item
            JOIN FETCH item.movie
            JOIN FETCH item.user
            WHERE item.id.userId = :userId
            AND item.deletableState.deleted = false
            ORDER BY item.addedAt DESC, item.id.userId DESC
            """)
    List<RatedMovie> findAllActiveByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT item
            FROM RatedMovie item
            JOIN FETCH item.movie
            JOIN FETCH item.user
            WHERE item.id.userId = :userId
            AND item.deletableState.deleted = false
            ORDER BY item.addedAt DESC, item.id.movieId DESC
            """)
    Slice<RatedMovie> findRatedPageDescFirst(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM RatedMovie item
            JOIN FETCH item.movie
            JOIN FETCH item.user
            WHERE item.id.userId = :userId
            AND item.deletableState.deleted = false
            AND (
                item.addedAt < :cursorAddedAt
                OR (item.addedAt = :cursorAddedAt AND item.id.movieId < :cursorEntityId)
            )
            ORDER BY item.addedAt DESC, item.id.movieId DESC
            """)
    Slice<RatedMovie> findRatedPageDescAfter(
            @Param("userId") Long userId,
            @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
            @Param("cursorEntityId") Long cursorEntityId,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM RatedMovie item
            JOIN FETCH item.movie
            JOIN FETCH item.user
            WHERE item.id.userId = :userId
            AND item.deletableState.deleted = false
            ORDER BY item.addedAt ASC, item.id.movieId ASC
            """)
    Slice<RatedMovie> findRatedPageAscFirst(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM RatedMovie item
            JOIN FETCH item.movie
            JOIN FETCH item.user
            WHERE item.id.userId = :userId
            AND item.deletableState.deleted = false
            AND (
                item.addedAt > :cursorAddedAt
                OR (item.addedAt = :cursorAddedAt AND item.id.movieId > :cursorEntityId)
            )
            ORDER BY item.addedAt ASC, item.id.movieId ASC
            """)
    Slice<RatedMovie> findRatedPageAscAfter(
            @Param("userId") Long userId,
            @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
            @Param("cursorEntityId") Long cursorEntityId,
            Pageable pageable
    );
}