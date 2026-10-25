package com.example.demo.controller.linked_collection;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.service.cursor.FavouritesCursorService;
import com.example.demo.service.linked_collection.FavouritesService;
import com.example.demo.web.resolver.CurrentUserId;
import com.example.demo.web.util.ApiResponseFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/favourites")
public class FavouritesController extends AbstractLinkedCollectionController<FavouriteMovieViewDTO> {

    private static final int SUGGESTIONS_LIMIT = 10;

    private final FavouritesCursorService favouritesCursorService;

    public FavouritesController(FavouritesService favouritesService,
                                FavouritesCursorService favouritesCursorService,
                                ApiResponseFactory apiResponseFactory) {
        super(favouritesService, favouritesCursorService, apiResponseFactory, "account/favourites");
        this.favouritesCursorService = favouritesCursorService;
    }

    @Override
    public String getCollection(@ModelAttribute CursorPageRequest cursorPageRequest,
                                @CurrentUserId Long userId,
                                Model model) {
        populateCollectionModel(userId, cursorPageRequest, model);
        return viewName;
    }

    @GetMapping("/suggestions")
    @ResponseBody
    public List<String> suggestions(@RequestParam(value = "query", required = false, defaultValue = "") String query,
                                    @CurrentUserId(required = false) Long userId) {
        if (userId == null) return List.of();
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) return List.of();
        return favouritesCursorService.findTitlesStartingWith(userId, trimmed, PageRequest.of(0, SUGGESTIONS_LIMIT));
    }
}