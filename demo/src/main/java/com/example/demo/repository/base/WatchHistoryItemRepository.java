package com.example.demo.repository.base;

import com.example.demo.model.base.WatchHistoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchHistoryItemRepository extends JpaRepository<WatchHistoryItem, Long> {

    @Query("""
            SELECT item
            FROM WatchHistoryItem item
            WHERE item.id = :itemId
            AND item.purchasedMovie.user.id = :userId
            """)
    Optional<WatchHistoryItem> findByIdAndUserId(@Param("itemId") Long itemId,
                                                 @Param("userId") Long userId);
}