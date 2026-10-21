package com.example.demo.service.linked_collection;

import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.entity.FavouritesMapper;
import com.example.demo.model.entity.FavouriteMovie;
import com.example.demo.repository.entity.FavouriteMovieRepository;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.repository.entity.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavouritesService
        extends AbstractLinkedCollectionService<FavouriteMovie, FavouriteMovieViewDTO> {

    public FavouritesService(FavouriteMovieRepository favouriteMovieRepository,
                             UserRepository userRepository,
                             MovieRepository movieRepository,
                             UserStatusResolver userStatusResolver,
                             FavouritesMapper favouritesMapper) {
        super(favouriteMovieRepository,
                userRepository,
                movieRepository,
                userStatusResolver,
                () -> FavouriteMovie.builder().build(),
                favouritesMapper::toOwnerView,
                ErrorCode.EMPTY_FAVOURITES);
    }

    @Override
    @Transactional
    public void add(Long movieId, Long userId) {
        if (existsInCollection(movieId, userId)) {
            return;
        }
        save(movieId, userId);
    }
}