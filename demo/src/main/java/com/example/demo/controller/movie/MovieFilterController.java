package com.example.demo.controller.movie;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.dto.movie.range.MovieFilterMetaResponse;
import com.example.demo.dto.movie.range.MovieRangesSnapshot;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.service.movie.MovieFilterMetaService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/filters")
@AllArgsConstructor
public class MovieFilterController {

    private final MovieFilterMetaService movieFilterMetaService;

    @GetMapping("/meta")
    public ResponseEntity<MovieFilterMetaResponse> meta() {
        MovieRangesSnapshot movieRangesSnapshot = movieFilterMetaService.getRanges();
        return ResponseEntity.ok(MovieFilterMetaResponse.builder()
                .genres(movieFilterMetaService.getAllGenres())
                .directors(List.of())
                .movieRangesSnapshot(movieRangesSnapshot)
                .build());
    }

    @GetMapping("/directors/search")
    public ResponseEntity<CursorPageResponse<String>> searchDirectors(
            @ModelAttribute CursorPageRequest cursorPageRequest) {

        Slice<String> slice = movieFilterMetaService.searchDirectors(cursorPageRequest);
        return ResponseEntity.ok(toResponse(slice));
    }

    @GetMapping("/genres/search")
    public ResponseEntity<CursorPageResponse<String>> searchGenres(
            @ModelAttribute CursorPageRequest cursorPageRequest) {

        Slice<String> slice = movieFilterMetaService.searchGenres(cursorPageRequest);
        return ResponseEntity.ok(toResponse(slice));
    }

    private CursorPageResponse<String> toResponse(Slice<String> slice) {
        List<String> items = slice.getContent();
        boolean hasMore = slice.hasNext();

        String nextCursor = null;
        if (hasMore && !items.isEmpty()) {
            nextCursor = items.get(items.size() - 1);
        }

        return CursorPageResponse.<String>builder()
                .items(items)
                .hasMore(hasMore)
                .nextCursor(nextCursor)
                .sort("asc")
                .build();
    }
}