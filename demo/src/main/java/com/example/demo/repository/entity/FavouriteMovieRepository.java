package com.example.demo.repository.entity;

import com.example.demo.model.entity.FavouriteMovie;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface FavouriteMovieRepository extends AbstractCatalogItemRepository<FavouriteMovie> {

    @Query("""
            SELECT item
            FROM FavouriteMovie item
            JOIN FETCH item.movie
            WHERE item.user.id = :userId
            ORDER BY item.addedAt DESC, item.id.movieId DESC
            """)
    Slice<FavouriteMovie> findPageDescFirst(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT item
            FROM FavouriteMovie item
            JOIN FETCH item.movie
            WHERE item.user.id = :userId
            ORDER BY item.addedAt ASC, item.id.movieId ASC
            """)
    Slice<FavouriteMovie> findPageAscFirst(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT item
            FROM FavouriteMovie item
            JOIN FETCH item.movie
            WHERE item.user.id = :userId
            AND (item.addedAt < :addedAt
                 OR (item.addedAt = :addedAt AND item.id.movieId < :entityId))
            ORDER BY item.addedAt DESC, item.id.movieId DESC
            """)
    Slice<FavouriteMovie> findPageDescAfter(@Param("userId") Long userId,
                                            @Param("addedAt") LocalDateTime addedAt,
                                            @Param("entityId") Long entityId,
                                            Pageable pageable);

    @Query("""
            SELECT item
            FROM FavouriteMovie item
            JOIN FETCH item.movie
            WHERE item.user.id = :userId
            AND (item.addedAt > :addedAt
                 OR (item.addedAt = :addedAt AND item.id.movieId > :entityId))
            ORDER BY item.addedAt ASC, item.id.movieId ASC
            """)
    Slice<FavouriteMovie> findPageAscAfter(@Param("userId") Long userId,
                                           @Param("addedAt") LocalDateTime addedAt,
                                           @Param("entityId") Long entityId,
                                           Pageable pageable);
}