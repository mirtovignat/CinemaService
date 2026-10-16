// demo/src/main/java/com/example/demo/repository/base/WatchHistoryItemRepository.java
package com.example.demo.repository.base;

import com.example.demo.model.base.WatchHistoryItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
            AND (:cursorId IS NULL OR item.id < :cursorId)
            ORDER BY item.id DESC
            """)
    Slice<WatchHistoryItem> findPageDesc(@Param("userId") Long userId,
                                         @Param("cursorId") Long cursorId,
                                         Pageable pageable);

    @Query("""
            SELECT item FROM WatchHistoryItem item
            JOIN FETCH item.purchasedMovie pm
            JOIN FETCH pm.movie
            WHERE pm.user.id = :userId
            AND (:cursorId IS NULL OR item.id > :cursorId)
            ORDER BY item.id ASC
            """)
    Slice<WatchHistoryItem> findPageAsc(@Param("userId") Long userId,
                                        @Param("cursorId") Long cursorId,
                                        Pageable pageable);
}