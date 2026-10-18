package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.HistoricalMovieViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.cursor.HistoryCursorService;
import com.example.demo.service.movie.FilterMetaService;
import com.example.demo.service.user.UserQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/history")
public class HistoryController extends AbstractCatalogPageController<HistoricalMovieViewDTO> {

    public HistoryController(HistoryCursorService historyCursorService,
                             FilterMetaService filterMetaService,
                             UserQueryService userQueryService,
                             UserMapper userMapper) {
        super(historyCursorService, filterMetaService, userQueryService, userMapper, "account/history");
    }
}