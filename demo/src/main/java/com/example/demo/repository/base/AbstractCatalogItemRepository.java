package com.example.demo.repository.base;

import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.model.base.AbstractCatalogItemId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface AbstractCatalogItemRepository<Entity extends AbstractCatalogItem>
        extends JpaRepository<Entity, AbstractCatalogItemId> {

    @Query("""
            SELECT item FROM #{#entityName} item
            JOIN FETCH item.movie
            WHERE item.id.userId = :userId
            """)
    List<Entity> findAllByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(item) > 0 FROM #{#entityName} item
            WHERE item.id.movieId = :movieId AND item.id.userId = :userId
            """)
    boolean existsByMovieIdAndUserId(@Param("movieId") Long movieId,
                                     @Param("userId") Long userId);

    @Query("""
            SELECT item FROM #{#entityName} item
            JOIN FETCH item.movie
            WHERE item.id.movieId = :movieId AND item.id.userId = :userId
            """)
    Optional<Entity> findByMovieIdAndUserId(@Param("movieId") Long movieId,
                                            @Param("userId") Long userId);

    @Modifying
    @Query("""
            DELETE FROM #{#entityName} item
            WHERE item.id.movieId = :movieId AND item.id.userId = :userId
            """)
    void deleteByMovieIdAndUserId(@Param("movieId") Long movieId,
                                  @Param("userId") Long userId);

    @Modifying
    @Query("""
            DELETE FROM #{#entityName} item
            WHERE item.id.userId = :userId
            """)
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(item) FROM #{#entityName} item
            WHERE item.id.userId = :userId
            """)
    Long countByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT item.id.movieId FROM #{#entityName} item
            WHERE item.id.userId = :userId
            """)
    List<Long> findMovieIdsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT item.id.movieId FROM #{#entityName} item
            WHERE item.id.userId = :userId
            AND item.id.movieId IN :movieIds
            """)
    List<Long> findMovieIdsByUserIdAndMovieIdIn(@Param("userId") Long userId,
                                                @Param("movieIds") Collection<Long> movieIds);

    @Query("""
            SELECT item FROM #{#entityName} item
            JOIN FETCH item.movie
            WHERE item.id.userId = :userId
            AND (
                :query IS NULL
                OR TRANSLATE(LOWER(item.movie.title), 'ё', 'е') LIKE CONCAT(CAST(:query AS string), '%') ESCAPE '\\'
            )
            AND (
                :hasCursor = false
                OR item.addedAt > :cursorAddedAt
                OR (item.addedAt = :cursorAddedAt AND item.id.movieId > :cursorMovieId)
            )
            ORDER BY item.addedAt ASC, item.id.movieId ASC
            """)
    Slice<Entity> fetchAscending(@Param("userId") Long userId,
                                 @Param("hasCursor") boolean hasCursor,
                                 @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                 @Param("cursorMovieId") Long cursorMovieId,
                                 @Param("query") String query,
                                 Pageable pageable);

    @Query("""
            SELECT item FROM #{#entityName} item
            JOIN FETCH item.movie
            WHERE item.id.userId = :userId
            AND (
                :query IS NULL
                OR TRANSLATE(LOWER(item.movie.title), 'ё', 'е') LIKE CONCAT(CAST(:query AS string), '%') ESCAPE '\\'
            )
            AND (
                :hasCursor = false
                OR item.addedAt < :cursorAddedAt
                OR (item.addedAt = :cursorAddedAt AND item.id.movieId < :cursorMovieId)
            )
            ORDER BY item.addedAt DESC, item.id.movieId DESC
            """)
    Slice<Entity> fetchDescending(@Param("userId") Long userId,
                                  @Param("hasCursor") boolean hasCursor,
                                  @Param("cursorAddedAt") LocalDateTime cursorAddedAt,
                                  @Param("cursorMovieId") Long cursorMovieId,
                                  @Param("query") String query,
                                  Pageable pageable);

    @Query("""
            SELECT item.movie.title FROM #{#entityName} item
            WHERE item.id.userId = :userId
            AND TRANSLATE(LOWER(item.movie.title), 'ё', 'е') LIKE CONCAT(CAST(:prefix AS string), '%') ESCAPE '\\'
            ORDER BY item.movie.title ASC
            """)
    List<String> findTitlesStartingWith(@Param("userId") Long userId,
                                        @Param("prefix") String prefix,
                                        Pageable pageable);
}