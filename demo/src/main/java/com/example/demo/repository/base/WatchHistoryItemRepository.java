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
            SELECT watchHistoryItem
            FROM WatchHistoryItem watchHistoryItem
            WHERE watchHistoryItem.id = :itemId
            AND watchHistoryItem.purchasedMovie.user.id = :userId
            """)
    Optional<WatchHistoryItem> findByIdAndUserId(@Param("itemId") Long itemId,
                                                 @Param("userId") Long userId);

    @Query("""
            SELECT watchHistoryItem
            FROM WatchHistoryItem watchHistoryItem
            JOIN FETCH watchHistoryItem.purchasedMovie purchasedMovie
            JOIN FETCH purchasedMovie.movie
            WHERE purchasedMovie.user.id = :userId
            AND (
                :hasCursor = false
                OR watchHistoryItem.watchedAt > :cursorWatchedAt
                OR (watchHistoryItem.watchedAt = :cursorWatchedAt AND watchHistoryItem.id > :cursorId)
            )
            ORDER BY watchHistoryItem.watchedAt ASC, watchHistoryItem.id ASC
            """)
    Slice<WatchHistoryItem> fetchAscending(@Param("userId") Long userId,
                                           @Param("hasCursor") boolean hasCursor,
                                           @Param("cursorWatchedAt") LocalDateTime cursorWatchedAt,
                                           @Param("cursorId") Long cursorId,
                                           Pageable pageable);

    @Query("""
            SELECT watchHistoryItem
            FROM WatchHistoryItem watchHistoryItem
            JOIN FETCH watchHistoryItem.purchasedMovie purchasedMovie
            JOIN FETCH purchasedMovie.movie
            WHERE purchasedMovie.user.id = :userId
            AND (
                :hasCursor = false
                OR watchHistoryItem.watchedAt < :cursorWatchedAt
                OR (watchHistoryItem.watchedAt = :cursorWatchedAt AND watchHistoryItem.id < :cursorId)
            )
            ORDER BY watchHistoryItem.watchedAt DESC, watchHistoryItem.id DESC
            """)
    Slice<WatchHistoryItem> fetchDescending(@Param("userId") Long userId,
                                            @Param("hasCursor") boolean hasCursor,
                                            @Param("cursorWatchedAt") LocalDateTime cursorWatchedAt,
                                            @Param("cursorId") Long cursorId,
                                            Pageable pageable);
}