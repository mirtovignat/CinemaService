package com.example.demo.model.entity;

import com.example.demo.model.base.IdentifiableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "movies")
public class Movie extends IdentifiableEntity {

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "age_rating", nullable = false)
    private Integer ageRating;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "duration_seconds")
    private Duration duration;

    @Column(name = "genre", nullable = false)
    private String genre;

    @DecimalMin(value = "0.1", message = "Рейтинг не может быть меньше 0.1")
    @DecimalMax(value = "10.0", message = "Рейтинг не может быть больше 10.0")
    @Column(name = "rating", precision = 4, scale = 2)
    private BigDecimal rating;

    @Builder.Default
    @Column(name = "rating_sum", nullable = false, precision = 14, scale = 2)
    private BigDecimal ratingSum = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "rating_count", nullable = false)
    private Long ratingCount = 0L;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "director", columnDefinition = "text")
    private String director;

    @Builder.Default
    @BatchSize(size = 35)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "movie_actors",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "actor_id")
    )
    private Set<Actor> actors = new LinkedHashSet<>();

    @Builder.Default
    @BatchSize(size = 35)
    @OneToMany(mappedBy = "movie", fetch = FetchType.LAZY)
    private Set<CartItem> cartItems = new LinkedHashSet<>();

    @Builder.Default
    @BatchSize(size = 35)
    @OneToMany(mappedBy = "movie", fetch = FetchType.LAZY)
    private Set<PurchasedMovie> purchases = new LinkedHashSet<>();

    @Builder.Default
    @BatchSize(size = 35)
    @OneToMany(mappedBy = "movie", fetch = FetchType.LAZY)
    private Set<FavouriteMovie> favourites = new LinkedHashSet<>();

    @Builder.Default
    @BatchSize(size = 35)
    @OneToMany(mappedBy = "movie", fetch = FetchType.LAZY)
    private Set<RatedMovie> ratings = new LinkedHashSet<>();
}