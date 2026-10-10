package com.example.demo.controller.linked_collection;

import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.model.entity.FavouriteMovie;
import com.example.demo.service.linked_collection.FavouritesService;
import com.example.demo.util.MessageUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/favourites")
public class FavouritesController extends AbstractLinkedCollectionController<FavouriteMovie, FavouriteMovieViewDTO> {

    public FavouritesController(FavouritesService favouritesService, MessageUtils messageUtils) {
        super("favourites", favouritesService, messageUtils);
    }
}