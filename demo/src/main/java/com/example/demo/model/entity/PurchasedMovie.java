package com.example.demo.model.entity;

import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.model.base.WatchHistoryItem;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "purchased_movies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "movie_id"}))
public class PurchasedMovie extends AbstractCatalogItem {

    @Column(name = "price_snapshot", nullable = false)
    private BigDecimal priceSnapshot;

    @Builder.Default
    @BatchSize(size = 20)
    @OneToMany(mappedBy = "purchasedMovie", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WatchHistoryItem> watchedHistory = new ArrayList<>();

    public void addWatchHistory(LocalDateTime time) {
        if (getUser() != null && !getUser().isSaveHistory()) {
            return;
        }
        watchedHistory.add(WatchHistoryItem.of(this, time));
    }

    public void clearHistory() {
        this.watchedHistory.clear();
    }

}