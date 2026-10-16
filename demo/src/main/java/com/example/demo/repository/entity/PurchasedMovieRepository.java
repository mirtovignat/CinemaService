// demo/src/main/java/com/example/demo/repository/entity/PurchasedMovieRepository.java
package com.example.demo.repository.entity;

import com.example.demo.model.entity.PurchasedMovie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PurchasedMovieRepository extends JpaRepository<PurchasedMovie, Long> {

    @Query("""
            SELECT purchasedMovie
            FROM PurchasedMovie purchasedMovie
            JOIN FETCH purchasedMovie.movie
            WHERE purchasedMovie.user.id = :userId
            AND (:cursorMovieId IS NULL OR purchasedMovie.id.movieId < :cursorMovieId)
            ORDER BY purchasedMovie.id.movieId DESC
            """)
    Slice<PurchasedMovie> findPageDesc(@Param("userId") Long userId,
                                       @Param("cursorMovieId") Long cursorMovieId,
                                       Pageable pageable);

    @Query("""
            SELECT purchasedMovie
            FROM PurchasedMovie purchasedMovie
            JOIN FETCH purchasedMovie.movie
            WHERE purchasedMovie.user.id = :userId
            AND (:cursorMovieId IS NULL OR purchasedMovie.id.movieId > :cursorMovieId)
            ORDER BY purchasedMovie.id.movieId ASC
            """)
    Slice<PurchasedMovie> findPageAsc(@Param("userId") Long userId,
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