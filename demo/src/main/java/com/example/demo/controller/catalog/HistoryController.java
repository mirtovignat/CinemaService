package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.HistoricalMovieViewDTO;
import com.example.demo.service.catalog.HistoryService;
import com.example.demo.service.movie.FilterMetaService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/history")
public class HistoryController extends AbstractGroupedHistoryController<HistoricalMovieViewDTO> {

    public HistoryController(HistoryService historyService, FilterMetaService filterMetaService) {
        super("catalog/history", filterMetaService, historyService::getGroupedHistory);
    }
}