package com.example.demo.model.base;

import com.example.demo.model.entity.PurchasedMovie;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "watch_history")
public class WatchHistoryItem extends IdentifiableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false),
            @JoinColumn(name = "movie_id", referencedColumnName = "movie_id", nullable = false)
    })
    private PurchasedMovie purchasedMovie;

    @Column(name = "watched_at", nullable = false)
    private LocalDateTime watchedAt;

    public static WatchHistoryItem of(PurchasedMovie purchasedMovie, LocalDateTime watchedAt) {
        return WatchHistoryItem.builder()
                .purchasedMovie(purchasedMovie)
                .watchedAt(watchedAt)
                .build();
    }
}