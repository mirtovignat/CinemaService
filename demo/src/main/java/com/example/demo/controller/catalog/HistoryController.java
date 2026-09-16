package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.HistoricalMovieViewDTO;
import com.example.demo.dto.response.GroupedHistoryResponse;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.service.catalog.HistoryService;
import com.example.demo.service.movie.MovieService;
import com.example.demo.service.user.UserQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/history")
public class HistoryController extends AbstractGroupedHistoryController<PurchasedMovie, HistoricalMovieViewDTO> {

    private final HistoryService historyService;

    public HistoryController(
            HistoryService historyService,
            MovieService movieService,
            UserQueryService userQueryService
    ) {
        super("catalog/history", historyService, movieService, userQueryService);
        this.historyService = historyService;
    }

    @Override
    protected GroupedHistoryResponse<HistoricalMovieViewDTO> getGroupedHistory(Long userId) {
        return historyService.getGroupedHistory(userId);
    }
}