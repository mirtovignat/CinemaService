package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.dto.response.GroupedHistoryResponse;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.service.catalog.RatedCatalogService;
import com.example.demo.service.movie.MovieService;
import com.example.demo.service.user.UserQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rated/history")
public class RatedCatalogController extends AbstractGroupedHistoryController<RatedMovie, RatedMovieViewDTO> {

    private final RatedCatalogService ratedCatalogService;

    public RatedCatalogController(
            RatedCatalogService ratedCatalogService,
            MovieService movieService,
            UserQueryService userQueryService
    ) {
        super("catalog/rated", ratedCatalogService, movieService, userQueryService);
        this.ratedCatalogService = ratedCatalogService;
    }

    @Override
    protected GroupedHistoryResponse<RatedMovieViewDTO> getGroupedHistory(Long userId) {
        return ratedCatalogService.getGroupedRatedHistory(userId);
    }
}