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
            WHERE LOWER(TRANSLATE(full_name, 'ёЁ', 'еЕ')) LIKE LOWER(CONCAT(:searchQuery, '%')) ESCAPE '\\'
              AND (
                  :hasCursor = false
                  OR LOWER(TRANSLATE(full_name, 'ёЁ', 'еЕ')) > LOWER(TRANSLATE(:cursor, 'ёЁ', 'еЕ'))
              )
            ORDER BY LOWER(TRANSLATE(full_name, 'ёЁ', 'еЕ')) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<String> fetchActorsKeyset(
            @Param("searchQuery") String searchQuery,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursor") String cursor,
            @Param("limit") int limit
    );
}