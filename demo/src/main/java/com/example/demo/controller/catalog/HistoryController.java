package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.HistoricalMovieViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.cursor.HistoryCursorService;
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

    private static final int SUGGESTIONS_LIMIT = 10;

    private final HistoryCursorService historyCursorService;

    public HistoryController(HistoryCursorService historyCursorService,
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
    }

    @GetMapping("/suggestions")
    @ResponseBody
    public List<String> suggestions(@RequestParam(value = "query", required = false, defaultValue = "") String query,
                                    @CurrentUserId(required = false) Long userId) {
        if (userId == null) return List.of();
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) return List.of();
        return historyCursorService.findTitlesStartingWith(userId, trimmed, PageRequest.of(0, SUGGESTIONS_LIMIT));
    }
}