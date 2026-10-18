package com.example.demo.controller.linked_collection;

import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.service.cursor.FavouritesCursorService;
import com.example.demo.service.linked_collection.FavouritesService;
import com.example.demo.web.util.ApiResponseFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/favourites")
public class FavouritesController extends AbstractLinkedCollectionController<FavouriteMovieViewDTO> {

    public FavouritesController(FavouritesService favouritesService,
                                FavouritesCursorService favouritesCursorService,
                                ApiResponseFactory apiResponseFactory) {
        super(favouritesService, favouritesCursorService, apiResponseFactory, "account/favourites");
    }
}