package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.cursor.RatedCursorService;
import com.example.demo.service.cursor.SuggestionService;
import com.example.demo.service.movie.MovieFilterMetaService;
import com.example.demo.service.user.UserQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rated/history")
public class RatedCatalogController extends AbstractCatalogPageController<RatedMovieViewDTO> {

    public RatedCatalogController(RatedCursorService ratedCursorService,
                                  SuggestionService suggestionService,
                                  MovieFilterMetaService movieFilterMetaService,
                                  UserQueryService userQueryService,
                                  UserMapper userMapper) {
        super(ratedCursorService,
                movieFilterMetaService,
                userQueryService,
                userMapper,
                suggestionService,
                "account/rated",
                "ratedHistoryPageResponse");
    }
}