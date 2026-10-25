package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.HistoricalMovieViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.cursor.HistoryCursorService;
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
@RequestMapping("/history")
public class HistoryController extends AbstractCatalogPageController<HistoricalMovieViewDTO> {

    private final HistoryCursorService historyCursorService;
    private final SuggestionService suggestionService;

    public HistoryController(HistoryCursorService historyCursorService,
                             SuggestionService suggestionService,
                             MovieFilterMetaService movieFilterMetaService,
                             UserQueryService userQueryService,
                             UserMapper userMapper) {
        super(historyCursorService,
                movieFilterMetaService,
                userQueryService,
                userMapper,
                "account/history"
        );
        this.historyCursorService = historyCursorService;
        this.suggestionService = suggestionService;
    }

    @GetMapping("/suggestions")
    @ResponseBody
    public List<String> suggestions(@RequestParam(value = "query", required = false, defaultValue = "") String query,
                                    @CurrentUserId(required = false) Long userId) {
        return suggestionService.suggest(userId, query,
                (uId, normalizedQuery) -> historyCursorService.findTitlesStartingWith(uId, normalizedQuery, PageRequest.of(0, 10)));
    }
}