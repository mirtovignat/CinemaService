package com.example.demo.controller.catalog;

import com.example.demo.dto.response.CatalogResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.service.catalog.AbstractCatalogService;
import com.example.demo.service.movie.MovieService;
import com.example.demo.service.user.UserQueryService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@AllArgsConstructor
public class AbstractCatalogController<Entity extends AbstractCatalogItem, DTO> {

    @Getter
    private final String viewName;
    @Getter
    private final AbstractCatalogService<Entity, DTO> catalogService;
    private final MovieService movieService;
    private final UserQueryService userQueryService;

    @GetMapping
    public String getCatalog(
            Model model,
            @SessionAttribute SessionUser sessionUser
    ) {
        Long userId = sessionUser.id();
        CatalogResponse<DTO> response = catalogService.buildCatalogResponse(
                userQueryService.getUserForOwner(userId)
        );
        model.addAttribute("genres", movieService.getAllGenres());
        model.addAttribute("response", response);
        return getViewName();
    }
}