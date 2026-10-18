package com.example.demo.service.cursor;

import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.mapper.entity.FavouritesMapper;
import com.example.demo.model.entity.FavouriteMovie;
import com.example.demo.repository.entity.FavouriteMovieRepository;
import com.example.demo.service.cursor.util.CursorCodec;
import com.example.demo.service.cursor.util.KeyUtils;
import com.example.demo.service.cursor.util.UserStatusMapperHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FavouritesCursorService extends AbstractUserCursorService<FavouriteMovie, FavouriteMovieViewDTO> {

    public FavouritesCursorService(CursorCodec cursorCodec,
                                   FavouriteMovieRepository favouriteMovieRepository,
                                   UserStatusMapperHelper userStatusMapperHelper,
                                   FavouritesMapper favouritesMapper,
                                   @Value("${app.catalog.page-size:20}") int defaultPageSize) {
        super(cursorCodec,
                defaultPageSize,
                favouriteMovie -> KeyUtils.encodeTimestampAndIdentifier(
                        favouriteMovie.getAddedAt(), favouriteMovie.getAbstractCatalogItemId().getMovieId()),
                favouriteMovies -> userStatusMapperHelper.mapWithStatuses(favouriteMovies, favouritesMapper::toOwnerView),
                favouriteMovieRepository::fetchAscending,
                favouriteMovieRepository::fetchDescending);
    }
}