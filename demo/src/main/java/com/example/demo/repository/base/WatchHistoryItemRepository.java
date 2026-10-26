package com.example.demo.repository.base;

import com.example.demo.model.base.WatchHistoryItem;
import com.example.demo.repository.projection.WatchedSuggestionProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WatchHistoryItemRepository extends JpaRepository<WatchHistoryItem, Long> {

    @Modifying
    @Query("""
            DELETE FROM WatchHistoryItem watchHistoryItem
            WHERE watchHistoryItem.purchasedMovie.user.id = :userId
            """)
    void deleteAllByUserId(@Param("userId") Long userId);

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
                :query IS NULL
                OR TRANSLATE(LOWER(purchasedMovie.movie.title), 'ё', 'е') LIKE CONCAT(CAST(:query AS string), '%') ESCAPE '\\'
            )
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
                                           @Param("query") String query,
                                           Pageable pageable);

    @Query("""
            SELECT watchHistoryItem
            FROM WatchHistoryItem watchHistoryItem
            JOIN FETCH watchHistoryItem.purchasedMovie purchasedMovie
            JOIN FETCH purchasedMovie.movie
            WHERE purchasedMovie.user.id = :userId
            AND (
                :query IS NULL
                OR TRANSLATE(LOWER(purchasedMovie.movie.title), 'ё', 'е') LIKE CONCAT(CAST(:query AS string), '%') ESCAPE '\\'
            )
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
                                            @Param("query") String query,
                                            Pageable pageable);

    @Query("""
            SELECT purchasedMovie.movie.id AS movieId,
                   purchasedMovie.movie.title AS title,
                   COUNT(watchHistoryItem.id) AS watchCount,
                   MAX(watchHistoryItem.watchedAt) AS lastWatchedAt
            FROM WatchHistoryItem watchHistoryItem
            JOIN watchHistoryItem.purchasedMovie purchasedMovie
            WHERE purchasedMovie.user.id = :userId
            AND TRANSLATE(LOWER(purchasedMovie.movie.title), 'ё', 'е') LIKE CONCAT(CAST(:prefix AS string), '%') ESCAPE '\\'
            GROUP BY purchasedMovie.movie.id, purchasedMovie.movie.title
            ORDER BY MAX(watchHistoryItem.watchedAt) DESC, purchasedMovie.movie.id ASC
            """)
    List<WatchedSuggestionProjection> findGroupedWatchedSuggestionsStartingWith(
            @Param("userId") Long userId,
            @Param("prefix") String prefix,
            Pageable pageable);
}