package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.service.catalog.RatedCatalogService;
import com.example.demo.service.movie.FilterMetaService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rated/history")
public class RatedCatalogController extends AbstractGroupedHistoryController<RatedMovieViewDTO> {

    public RatedCatalogController(RatedCatalogService ratedCatalogService, FilterMetaService filterMetaService) {
        super("catalog/rated", filterMetaService, ratedCatalogService::getGroupedHistory);
    }
}