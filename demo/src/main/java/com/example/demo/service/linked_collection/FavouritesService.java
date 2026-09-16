package com.example.demo.service.linked_collection;

import com.example.demo.dto.badges.BadgeCountsDTO;
import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.entity.FavouritesMapper;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.model.entity.FavouriteMovie;
import com.example.demo.model.entity.Movie;
import com.example.demo.model.entity.User;
import com.example.demo.repository.entity.FavouriteMovieRepository;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.service.precence.FavouritePresenceService;
import org.springframework.stereotype.Service;

@Service
public class FavouritesService extends AbstractLinkedCollectionService<FavouriteMovie,
        FavouriteMovieViewDTO> {

    private final FavouritesMapper favouritesMapper;
    private final FavouritePresenceService favouriteMoviePresenceService;

    public FavouritesService(FavouriteMovieRepository favouriteMovieRepository,
                             UserRepository userRepository,
                             MovieRepository movieRepository,
                             PurchasedMovieRepository purchasedMovieRepository,
                             FavouritesMapper favouritesMapper,
                             FavouritePresenceService favouriteMoviePresenceService) {
        super(favouriteMovieRepository, userRepository, movieRepository,
                purchasedMovieRepository);
        this.favouritesMapper = favouritesMapper;
        this.favouriteMoviePresenceService = favouriteMoviePresenceService;
    }

    @Override
    public BadgeCountsDTO updateBadge(BadgeCountsDTO badgeCountsDTO, Long newCount) {
        return badgeCountsDTO.toBuilder().favouritesCount(newCount).build();
    }

    @Override
    protected FavouriteMovie createEntity(User user, Movie movie) {
        return AbstractCatalogItem.init(FavouriteMovie.builder()
                .build(), user, movie);
    }

    @Override
    protected ErrorCode getEmptyErrorCode() {
        return ErrorCode.EMPTY_FAVOURITES;
    }

    @Override
    protected FavouriteMovieViewDTO mapToDto(FavouriteMovie favouriteMovie) {
        return favouritesMapper.toOwnerView(favouriteMovie, favouriteMoviePresenceService);
    }
}