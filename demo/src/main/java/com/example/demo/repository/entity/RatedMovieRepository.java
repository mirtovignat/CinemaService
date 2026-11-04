package com.example.demo.repository.entity;

import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import com.example.demo.repository.projection.MovieReviewCountProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatedMovieRepository extends AbstractCatalogItemRepository<RatedMovie> {

    @Override
    @Query("""
            SELECT DISTINCT ratedMovie.movie.title FROM RatedMovie ratedMovie
            WHERE ratedMovie.user.id = :userId
            AND LOWER(TRANSLATE(ratedMovie.movie.title, 'ёЁ', 'еЕ')) LIKE CONCAT(CAST(:prefix AS string), '%') ESCAPE '\\'
            ORDER BY ratedMovie.movie.title ASC
            """)
    List<String> findTitlesStartingWith(@Param("userId") Long userId,
                                        @Param("prefix") String prefix,
                                        Pageable pageable);

    @Query("""
            SELECT ratedMovie FROM RatedMovie ratedMovie
            JOIN FETCH ratedMovie.user
            WHERE ratedMovie.movie.id = :movieId
            AND (:viewerUserId IS NULL OR ratedMovie.user.id <> :viewerUserId)
            AND (:hasCursor = false OR ratedMovie.id > :cursorId)
            ORDER BY ratedMovie.id ASC
            """)
    Slice<RatedMovie> fetchReviewsAscending(@Param("movieId") Long movieId,
                                            @Param("viewerUserId") Long viewerUserId,
                                            @Param("hasCursor") boolean hasCursor,
                                            @Param("cursorId") Long cursorId,
                                            Pageable pageable);

    @Query("""
            SELECT ratedMovie FROM RatedMovie ratedMovie
            JOIN FETCH ratedMovie.user
            WHERE ratedMovie.movie.id = :movieId
            AND (:viewerUserId IS NULL OR ratedMovie.user.id <> :viewerUserId)
            AND (:hasCursor = false OR ratedMovie.id < :cursorId)
            ORDER BY ratedMovie.id DESC
            """)
    Slice<RatedMovie> fetchReviewsDescending(@Param("movieId") Long movieId,
                                             @Param("viewerUserId") Long viewerUserId,
                                             @Param("hasCursor") boolean hasCursor,
                                             @Param("cursorId") Long cursorId,
                                             Pageable pageable);

    @Override
    @Query("""
            SELECT DISTINCT ratedMovie FROM RatedMovie ratedMovie
            JOIN FETCH ratedMovie.movie
            JOIN FETCH ratedMovie.user
            WHERE ratedMovie.user.id = :userId
            """)
    List<RatedMovie> findAllByUserId(@Param("userId") Long userId);

    @Override
    @Query("""
            SELECT COUNT(ratedMovie) > 0 FROM RatedMovie ratedMovie
            WHERE ratedMovie.movie.id = :movieId
            AND ratedMovie.user.id = :userId
            """)
    boolean existsByMovieIdAndUserId(@Param("movieId") Long movieId,
                                     @Param("userId") Long userId);

    @Override
    @Query("""
            SELECT COUNT(ratedMovie)
            FROM RatedMovie ratedMovie
            WHERE ratedMovie.user.id = :userId
            """)
    Long countByUserId(@Param("userId") Long userId);

    @Override
    @Query("""
            SELECT ratedMovie.movie.id
            FROM RatedMovie ratedMovie
            WHERE ratedMovie.user.id = :userId
            """)
    List<Long> findMovieIdsByUserId(@Param("userId") Long userId);

    @Override
    @Query("""
            SELECT ratedMovie
            FROM RatedMovie ratedMovie
            JOIN FETCH ratedMovie.movie
            JOIN FETCH ratedMovie.user
            WHERE ratedMovie.movie.id = :movieId
            AND ratedMovie.user.id = :userId
            """)
    Optional<RatedMovie> findByMovieIdAndUserId(@Param("movieId") Long movieId,
                                                @Param("userId") Long userId);

    @Query("""
            SELECT COUNT(ratedMovie)
            FROM RatedMovie ratedMovie
            WHERE ratedMovie.movie.id = :movieId
            """)
    Long countByMovieId(@Param("movieId") Long movieId);

    @Query("""
            SELECT
                ratedMovie.movie.id AS movieId,
                COUNT(ratedMovie)  AS total
            FROM RatedMovie ratedMovie
            WHERE ratedMovie.movie.id IN :movieIds
            GROUP BY ratedMovie.movie.id
            """)
    List<MovieReviewCountProjection> countByMovieIds(@Param("movieIds") Collection<Long> movieIds);

    @Query("""
            SELECT ratedMovie FROM RatedMovie ratedMovie
            JOIN FETCH ratedMovie.movie
            WHERE ratedMovie.user.id = :userId
            ORDER BY ratedMovie.ratingValue DESC
            """)
    List<RatedMovie> findTopNByUserIdOrderByRatingValueDesc(
            @Param("userId") Long userId,
            Pageable pageable);
}