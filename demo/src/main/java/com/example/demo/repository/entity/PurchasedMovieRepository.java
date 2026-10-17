package com.example.demo.repository.entity;

import com.example.demo.model.entity.PurchasedMovie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchasedMovieRepository extends JpaRepository<PurchasedMovie, Long> {

    @Query("""
            SELECT purchasedMovie
            FROM PurchasedMovie purchasedMovie
            JOIN FETCH purchasedMovie.movie
            WHERE purchasedMovie.id.userId = :userId
            AND (
                :hasCursor = false
                OR purchasedMovie.addedAt > :cursorAddedAt
                OR (purchasedMovie.addedAt = :cursorAddedAt AND purchasedMovie.id.movieId > :cursorMovieId)
            )
            ORDER BY purchasedMovie.addedAt ASC, purchasedMovie.id.movieId ASC
            """)
    Slice<PurchasedMovie> fetchAscending(@Param("userId") Long userId,
                                         @Param("hasCursor") boolean hasCursor,
                                         @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                         @Param("cursorMovieId") Long cursorMovieId,
                                         Pageable pageable);

    @Query("""
            SELECT purchasedMovie
            FROM PurchasedMovie purchasedMovie
            JOIN FETCH purchasedMovie.movie
            WHERE purchasedMovie.id.userId = :userId
            AND (
                :hasCursor = false
                OR purchasedMovie.addedAt < :cursorAddedAt
                OR (purchasedMovie.addedAt = :cursorAddedAt AND purchasedMovie.id.movieId < :cursorMovieId)
            )
            ORDER BY purchasedMovie.addedAt DESC, purchasedMovie.id.movieId DESC
            """)
    Slice<PurchasedMovie> fetchDescending(@Param("userId") Long userId,
                                          @Param("hasCursor") boolean hasCursor,
                                          @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                          @Param("cursorMovieId") Long cursorMovieId,
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