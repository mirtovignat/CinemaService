package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.cursor.LibraryCursorService;
import com.example.demo.service.movie.FilterMetaService;
import com.example.demo.service.user.UserQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/library")
public class LibraryController extends AbstractCatalogPageController<LibrarianMovieViewDTO> {

    public LibraryController(LibraryCursorService libraryCursorService,
                             FilterMetaService filterMetaService,
                             UserQueryService userQueryService,
                             UserMapper userMapper) {
        super(libraryCursorService,
                filterMetaService,
                userQueryService,
                userMapper,
                "account/library"
        );
    }
}