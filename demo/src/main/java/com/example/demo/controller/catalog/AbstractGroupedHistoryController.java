package com.example.demo.controller.catalog;

import com.example.demo.dto.response.GroupedHistoryResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.service.catalog.AbstractCatalogService;
import com.example.demo.service.movie.MovieService;
import com.example.demo.service.user.UserQueryService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

public abstract class AbstractGroupedHistoryController<Entity extends AbstractCatalogItem, DTO>
        extends AbstractCatalogController<Entity, DTO> {

    private final MovieService movieService;

    public AbstractGroupedHistoryController(
            String viewName,
            AbstractCatalogService<Entity, DTO> catalogService,
            MovieService movieService,
            UserQueryService userQueryService
    ) {
        super(viewName, catalogService, movieService, userQueryService);
        this.movieService = movieService;
    }

    protected abstract GroupedHistoryResponse<DTO> getGroupedHistory(Long userId);

    @Override
    @GetMapping
    public String getCatalog(
            Model model,
            @SessionAttribute SessionUser sessionUser
    ) {
        Long userId = sessionUser.id();
        GroupedHistoryResponse<DTO> groupedResponse = getGroupedHistory(userId);
        model.addAttribute("groupedResponse", groupedResponse);
        model.addAttribute("genres", movieService.getAllGenres());
        return getViewName();
    }
}