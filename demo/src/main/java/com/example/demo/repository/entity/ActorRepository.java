package com.example.demo.repository.entity;

import com.example.demo.model.entity.Actor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActorRepository extends JpaRepository<Actor, Long> {

    @Query(value = """
            SELECT full_name
            FROM actors
            WHERE (' ' || LOWER(TRANSLATE(full_name, 'ёЁ', 'еЕ'))) LIKE :searchQuery ESCAPE '\\'
            ORDER BY LOWER(TRANSLATE(full_name, 'ёЁ', 'еЕ')) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<String> fetchActorsKeysetFirstPage(
            @Param("searchQuery") String searchQuery,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT full_name
            FROM actors
            WHERE (' ' || LOWER(TRANSLATE(full_name, 'ёЁ', 'еЕ'))) LIKE :searchQuery ESCAPE '\\'
              AND LOWER(TRANSLATE(full_name, 'ёЁ', 'еЕ')) > :cursor
            ORDER BY LOWER(TRANSLATE(full_name, 'ёЁ', 'еЕ')) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<String> fetchActorsKeysetWithCursor(
            @Param("searchQuery") String searchQuery,
            @Param("cursor") String cursor,
            @Param("limit") int limit
    );
}