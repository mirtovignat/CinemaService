package com.example.demo.repository.entity;

import com.example.demo.model.entity.PurchasedMovie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PurchasedMovieRepository extends JpaRepository<PurchasedMovie, Long> {

    @Query("""
            SELECT purchasedMovie
            FROM PurchasedMovie purchasedMovie
            JOIN FETCH purchasedMovie.movie
            WHERE purchasedMovie.user.id = :userId
            ORDER BY purchasedMovie.addedAt DESC, purchasedMovie.id.movieId DESC
            """)
    Slice<PurchasedMovie> findPageDescFirst(@Param("userId") Long userId,
                                            Pageable pageable);

    @Query("""
            SELECT purchasedMovie
            FROM PurchasedMovie purchasedMovie
            JOIN FETCH purchasedMovie.movie
            WHERE purchasedMovie.user.id = :userId
            ORDER BY purchasedMovie.addedAt ASC, purchasedMovie.id.movieId ASC
            """)
    Slice<PurchasedMovie> findPageAscFirst(@Param("userId") Long userId,
                                           Pageable pageable);

    @Query("""
            SELECT purchasedMovie
            FROM PurchasedMovie purchasedMovie
            JOIN FETCH purchasedMovie.movie
            WHERE purchasedMovie.user.id = :userId
            AND (purchasedMovie.addedAt < :addedAt
                 OR (purchasedMovie.addedAt = :addedAt AND purchasedMovie.id.movieId < :entityId))
            ORDER BY purchasedMovie.addedAt DESC, purchasedMovie.id.movieId DESC
            """)
    Slice<PurchasedMovie> findPageDescAfter(@Param("userId") Long userId,
                                            @Param("addedAt") LocalDateTime addedAt,
                                            @Param("entityId") Long entityId,
                                            Pageable pageable);

    @Query("""
            SELECT purchasedMovie
            FROM PurchasedMovie purchasedMovie
            JOIN FETCH purchasedMovie.movie
            WHERE purchasedMovie.user.id = :userId
            AND (purchasedMovie.addedAt > :addedAt
                 OR (purchasedMovie.addedAt = :addedAt AND purchasedMovie.id.movieId > :entityId))
            ORDER BY purchasedMovie.addedAt ASC, purchasedMovie.id.movieId ASC
            """)
    Slice<PurchasedMovie> findPageAscAfter(@Param("userId") Long userId,
                                           @Param("addedAt") LocalDateTime addedAt,
                                           @Param("entityId") Long entityId,
                                           Pageable pageable);

    Optional<PurchasedMovie> findByMovieIdAndUserId(Long movieId, Long userId);

    boolean existsByMovieIdAndUserId(Long movieId, Long userId);

    @Query("""
            SELECT purchasedMovie.id.movieId
            FROM PurchasedMovie purchasedMovie
            WHERE purchasedMovie.user.id = :userId
            """)
    List<Long> findMovieIdsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT purchasedMovie.id.movieId
            FROM PurchasedMovie purchasedMovie
            WHERE purchasedMovie.user.id = :userId
            AND purchasedMovie.id.movieId IN :movieIds
            """)
    List<Long> findMovieIdsByUserIdAndMovieIdIn(@Param("userId") Long userId,
                                                @Param("movieIds") Collection<Long> movieIds);

    @Query("""
            SELECT purchasedMovie
            FROM PurchasedMovie purchasedMovie
            WHERE purchasedMovie.user.id = :userId
            """)
    List<PurchasedMovie> findAllByUserId(@Param("userId") Long userId);

    Long countByUserId(Long userId);
}