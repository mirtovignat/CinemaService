package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.service.catalog.LibraryService;
import com.example.demo.service.movie.MovieService;
import com.example.demo.service.user.UserQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/library")
public class LibraryController extends AbstractCatalogController<PurchasedMovie, LibrarianMovieViewDTO> {

    public LibraryController(
            LibraryService libraryService,
            MovieService movieService,
            UserQueryService userQueryService
    ) {
        super("catalog/library", libraryService, movieService, userQueryService);
    }
}