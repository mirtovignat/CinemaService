package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.cursor.LibraryCursorService;
import com.example.demo.service.cursor.SuggestionService;
import com.example.demo.service.movie.MovieFilterMetaService;
import com.example.demo.service.user.UserQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/library")
public class LibraryController extends AbstractCatalogPageController<LibrarianMovieViewDTO> {

    public LibraryController(LibraryCursorService libraryCursorService,
                             SuggestionService suggestionService,
                             MovieFilterMetaService movieFilterMetaService,
                             UserQueryService userQueryService,
                             UserMapper userMapper) {
        super(libraryCursorService,
                movieFilterMetaService,
                userQueryService,
                userMapper,
                suggestionService,
                "account/library",
                "libraryPageResponse");
    }
}