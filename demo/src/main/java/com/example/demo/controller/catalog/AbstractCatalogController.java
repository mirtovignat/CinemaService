package com.example.demo.controller.catalog;

import com.example.demo.dto.response.CatalogResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.service.catalog.AbstractCatalogService;
import com.example.demo.service.movie.FilterMetaService;
import com.example.demo.service.user.UserQueryService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

public class AbstractCatalogController<Entity extends AbstractCatalogItem, DTO> {

    protected final String viewName;
    protected final AbstractCatalogService<Entity, DTO> catalogService;
    protected final FilterMetaService filterMetaService;
    protected final UserQueryService userQueryService;

    public AbstractCatalogController(String viewName,
                                     AbstractCatalogService<Entity, DTO> catalogService,
                                     FilterMetaService filterMetaService,
                                     UserQueryService userQueryService) {
        this.viewName = viewName;
        this.catalogService = catalogService;
        this.filterMetaService = filterMetaService;
        this.userQueryService = userQueryService;
    }

    @GetMapping
    public String getCatalog(Model model, SessionUser sessionUser) {
        Long userId = sessionUser.id();
        CatalogResponse<DTO> response = catalogService.buildCatalogResponse(
                userQueryService.getUserForOwner(userId)
        );
        model.addAttribute("genres", filterMetaService.getAllGenres());
        model.addAttribute("response", response);
        return viewName;
    }
}