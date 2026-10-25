package com.example.demo.controller.linked_collection;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.service.cursor.FavouritesCursorService;
import com.example.demo.service.cursor.SuggestionService;
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

    private final FavouritesCursorService favouritesCursorService;
    private final SuggestionService suggestionService;

    public FavouritesController(FavouritesService favouritesService,
                                FavouritesCursorService favouritesCursorService,
                                SuggestionService suggestionService,
                                ApiResponseFactory apiResponseFactory) {
        super(favouritesService, favouritesCursorService, apiResponseFactory, "account/favourites");
        this.favouritesCursorService = favouritesCursorService;
        this.suggestionService = suggestionService;
    }

    @Override
    public String getCollection(@ModelAttribute CursorPageRequest cursorPageRequest,
                                @CurrentUserId Long userId,
                                Model model) {
        populateCollectionModel(userId, cursorPageRequest, model);
        return viewName;
    }

    @Override
    @GetMapping("/suggestions")
    @ResponseBody
    public List<String> suggestions(@RequestParam(value = "query", required = false, defaultValue = "") String query,
                                    @CurrentUserId(required = false) Long userId) {
        return suggestionService.suggest(userId, query,
                (uId, normalizedQuery) -> favouritesCursorService.findTitlesStartingWith(uId, normalizedQuery, PageRequest.of(0, 10)));
    }
}