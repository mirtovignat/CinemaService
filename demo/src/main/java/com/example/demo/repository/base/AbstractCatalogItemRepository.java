package com.example.demo.repository.base;

import com.example.demo.model.base.AbstractCatalogItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface AbstractCatalogItemRepository<Entity extends AbstractCatalogItem>
        extends JpaRepository<Entity, Long> {

    @Query("""
            SELECT item FROM #{#entityName} item
            JOIN FETCH item.movie
            WHERE item.user.id = :userId
            """)
    List<Entity> findAllByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(item) > 0 FROM #{#entityName} item
            WHERE item.movie.id = :movieId AND item.user.id = :userId
            """)
    boolean existsByMovieIdAndUserId(@Param("movieId") Long movieId,
                                     @Param("userId") Long userId);

    @Query("""
            SELECT item FROM #{#entityName} item
            JOIN FETCH item.movie
            WHERE item.movie.id = :movieId AND item.user.id = :userId
            """)
    Optional<Entity> findByMovieIdAndUserId(@Param("movieId") Long movieId,
                                            @Param("userId") Long userId);

    @Modifying
    @Query("""
            DELETE FROM #{#entityName} item
            WHERE item.movie.id = :movieId AND item.user.id = :userId
            """)
    void deleteByMovieIdAndUserId(@Param("movieId") Long movieId,
                                  @Param("userId") Long userId);

    @Modifying
    @Query("""
            DELETE FROM #{#entityName} item
            WHERE item.user.id = :userId
            """)
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(item) FROM #{#entityName} item
            WHERE item.user.id = :userId
            """)
    Long countByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT item.movie.id FROM #{#entityName} item
            WHERE item.user.id = :userId
            """)
    List<Long> findMovieIdsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT item.movie.id FROM #{#entityName} item
            WHERE item.user.id = :userId
            AND item.movie.id IN :movieIds
            """)
    List<Long> findMovieIdsByUserIdAndMovieIdIn(@Param("userId") Long userId,
                                                @Param("movieIds") Collection<Long> movieIds);

    @Query("""
        SELECT item FROM #{#entityName} item
        JOIN FETCH item.movie
        WHERE item.user.id = :userId
        AND LOWER(TRANSLATE(item.movie.title, 'ёЁ', 'еЕ')) LIKE CONCAT(CAST(:searchQuery AS string), '%') ESCAPE '\\'
        AND (:hasCursor = false OR item.id > :cursorId)
        ORDER BY item.id ASC
        """)
    Slice<Entity> fetchAscending(@Param("userId") Long userId,
                                 @Param("hasCursor") boolean hasCursor,
                                 @Param("cursorId") Long cursorId,
                                 @Param("searchQuery") String query,
                                 Pageable pageable);

    @Query("""
        SELECT item FROM #{#entityName} item
        JOIN FETCH item.movie
        WHERE item.user.id = :userId
        AND LOWER(TRANSLATE(item.movie.title, 'ёЁ', 'еЕ')) LIKE CONCAT(CAST(:searchQuery AS string), '%') ESCAPE '\\'
        AND (:hasCursor = false OR item.id < :cursorId)
        ORDER BY item.id DESC
        """)
    Slice<Entity> fetchDescending(@Param("userId") Long userId,
                                  @Param("hasCursor") boolean hasCursor,
                                  @Param("cursorId") Long cursorId,
                                  @Param("searchQuery") String query,
                                  Pageable pageable);

    @Query("""
            SELECT item.movie.title FROM #{#entityName} item
            WHERE item.user.id = :userId
            AND LOWER(TRANSLATE(item.movie.title, 'ёЁ', 'еЕ')) LIKE CONCAT(CAST(:prefix AS string), '%') ESCAPE '\\'
            ORDER BY item.movie.title ASC
            """)
    List<String> findTitlesStartingWith(@Param("userId") Long userId,
                                        @Param("prefix") String prefix,
                                        Pageable pageable);
}