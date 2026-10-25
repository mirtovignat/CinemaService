package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.cursor.LibraryCursorService;
import com.example.demo.service.movie.MovieFilterMetaService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.web.resolver.CurrentUserId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/library")
public class LibraryController extends AbstractCatalogPageController<LibrarianMovieViewDTO> {

    private static final int SUGGESTIONS_LIMIT = 10;

    private final LibraryCursorService libraryCursorService;

    public LibraryController(LibraryCursorService libraryCursorService,
                             MovieFilterMetaService movieFilterMetaService,
                             UserQueryService userQueryService,
                             UserMapper userMapper) {
        super(libraryCursorService,
                movieFilterMetaService,
                userQueryService,
                userMapper,
                "account/library"
        );
        this.libraryCursorService = libraryCursorService;
    }

    @GetMapping("/suggestions")
    @ResponseBody
    public List<String> suggestions(@RequestParam(value = "query", required = false, defaultValue = "") String query,
                                    @CurrentUserId(required = false) Long userId) {
        if (userId == null) return List.of();
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) return List.of();
        return libraryCursorService.findTitlesStartingWith(userId, trimmed, PageRequest.of(0, SUGGESTIONS_LIMIT));
    }
}