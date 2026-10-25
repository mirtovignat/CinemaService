package com.example.demo.controller.linked_collection;

import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.dto.response.CollectionPageResponse;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.service.cursor.FavouritesCursorService;
import com.example.demo.service.cursor.SuggestionService;
import com.example.demo.service.linked_collection.FavouritesService;
import com.example.demo.web.util.ApiResponseFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/favourites")
public class FavouritesController extends AbstractLinkedCollectionController<FavouriteMovieViewDTO, CollectionPageResponse<FavouriteMovieViewDTO>> {

    public FavouritesController(FavouritesService favouritesService,
                                FavouritesCursorService favouritesCursorService,
                                SuggestionService suggestionService,
                                ApiResponseFactory apiResponseFactory) {
        super(favouritesService,
                favouritesCursorService,
                suggestionService,
                apiResponseFactory,
                "account/favourites",
                "favouritesPageResponse");
    }

    @Override
    protected CollectionPageResponse<FavouriteMovieViewDTO> buildResponse(CursorPageResponse<FavouriteMovieViewDTO>
                                                                                  cursorPageResponse,
                                                                          Long userId,
                                                                          String query) {
        return CollectionPageResponse.<FavouriteMovieViewDTO>builder()
                .cursorPageResponse(cursorPageResponse)
                .catalogCount(collectionService.getCount(userId))
                .query(query)
                .build();
    }
}