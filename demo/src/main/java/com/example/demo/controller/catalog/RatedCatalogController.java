package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.cursor.RatedCursorService;
import com.example.demo.service.cursor.SuggestionService;
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
@RequestMapping("/rated/history")
public class RatedCatalogController extends AbstractCatalogPageController<RatedMovieViewDTO> {

    private final RatedCursorService ratedCursorService;
    private final SuggestionService suggestionService;

    public RatedCatalogController(RatedCursorService ratedCursorService,
                                  SuggestionService suggestionService,
                                  MovieFilterMetaService movieFilterMetaService,
                                  UserQueryService userQueryService,
                                  UserMapper userMapper) {
        super(ratedCursorService,
                movieFilterMetaService,
                userQueryService,
                userMapper,
                "account/rated"
        );
        this.ratedCursorService = ratedCursorService;
        this.suggestionService = suggestionService;
    }

    @GetMapping("/suggestions")
    @ResponseBody
    public List<String> suggestions(@RequestParam(value = "query", required = false, defaultValue = "") String query,
                                    @CurrentUserId(required = false) Long userId) {
        return suggestionService.suggest(userId, query,
                (uId, normalizedQuery) -> ratedCursorService.findTitlesStartingWith(uId, normalizedQuery, PageRequest.of(0, 10)));
    }
}