package com.example.demo.model.base;

import com.example.demo.model.entity.Movie;
import com.example.demo.model.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public abstract class AbstractCatalogItem {

    @EmbeddedId
    private AbstractCatalogItemId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("movieId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    public static <Entity extends AbstractCatalogItem> Entity init(
            Entity entity,
            User user,
            Movie movie
    ) {
        entity.setAddedAt(LocalDateTime.now(ZoneOffset.UTC));
        entity.setUser(user);
        entity.setMovie(movie);
        AbstractCatalogItemId id = AbstractCatalogItemId.from(movie, user);
        entity.setId(id);
        return entity;
    }
}