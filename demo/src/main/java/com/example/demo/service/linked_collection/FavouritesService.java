package com.example.demo.service.linked_collection;

import com.example.demo.exception.ErrorCode;
import com.example.demo.model.entity.FavouriteMovie;
import com.example.demo.repository.entity.FavouriteMovieRepository;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.repository.entity.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class FavouritesService
        extends AbstractLinkedCollectionService<FavouriteMovie> {

    public FavouritesService(FavouriteMovieRepository favouriteMovieRepository,
                             UserRepository userRepository,
                             MovieRepository movieRepository) {
        super(favouriteMovieRepository,
                userRepository,
                movieRepository,
                () -> FavouriteMovie.builder().build(),
                ErrorCode.EMPTY_FAVOURITES);
    }

    @Override
    public void add(Long movieId, Long userId) {
        if (existsInCollection(movieId, userId)) {
            return;
        }
        FavouriteMovie preparedFavouriteMovie = prepare(movieId, userId);
        try {
            persist(preparedFavouriteMovie);
        } catch (DataIntegrityViolationException ignored) {
        }
    }
}