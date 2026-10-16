package com.example.demo.repository.base;

import com.example.demo.model.base.WatchHistoryItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface WatchHistoryItemRepository extends JpaRepository<WatchHistoryItem, Long> {

    @Query("""
            SELECT item FROM WatchHistoryItem item
            WHERE item.id = :itemId AND item.purchasedMovie.user.id = :userId
            """)
    Optional<WatchHistoryItem> findByIdAndUserId(@Param("itemId") Long itemId,
                                                 @Param("userId") Long userId);

    @Query("""
            SELECT item FROM WatchHistoryItem item
            JOIN FETCH item.purchasedMovie pm
            JOIN FETCH pm.movie
            WHERE pm.user.id = :userId
            ORDER BY item.watchedAt ASC, item.id ASC
            """)
    Slice<WatchHistoryItem> findPageAscFirst(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT item FROM WatchHistoryItem item
            JOIN FETCH item.purchasedMovie pm
            JOIN FETCH pm.movie
            WHERE pm.user.id = :userId
            ORDER BY item.watchedAt DESC, item.id DESC
            """)
    Slice<WatchHistoryItem> findPageDescFirst(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT item FROM WatchHistoryItem item
            JOIN FETCH item.purchasedMovie pm
            JOIN FETCH pm.movie
            WHERE pm.user.id = :userId
            AND (item.watchedAt > :cursorAddedAt
                 OR (item.watchedAt = :cursorAddedAt AND item.id > :cursorEntityId))
            ORDER BY item.watchedAt ASC, item.id ASC
            """)
    Slice<WatchHistoryItem> findPageAscAfter(@Param("userId") Long userId,
                                             @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                             @Param("cursorEntityId") Long cursorEntityId,
                                             Pageable pageable);

    @Query("""
            SELECT item FROM WatchHistoryItem item
            JOIN FETCH item.purchasedMovie pm
            JOIN FETCH pm.movie
            WHERE pm.user.id = :userId
            AND (item.watchedAt < :cursorAddedAt
                 OR (item.watchedAt = :cursorAddedAt AND item.id < :cursorEntityId))
            ORDER BY item.watchedAt DESC, item.id DESC
            """)
    Slice<WatchHistoryItem> findPageDescAfter(@Param("userId") Long userId,
                                              @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                              @Param("cursorEntityId") Long cursorEntityId,
                                              Pageable pageable);
}