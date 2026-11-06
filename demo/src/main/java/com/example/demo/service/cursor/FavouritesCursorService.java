package com.example.demo.service.cursor;

import com.example.demo.cursor.UserStatusMapperHelper;
import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.IdCodec;
import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.mapper.entity.FavouritesMapper;
import com.example.demo.model.entity.FavouriteMovie;
import com.example.demo.repository.entity.FavouriteMovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FavouritesCursorService extends AbstractUserCursorService<FavouriteMovie, FavouriteMovieViewDTO>
        implements SuggestableCursorService {

    private final FavouriteMovieRepository favouriteMovieRepository;

    public FavouritesCursorService(CursorCodec cursorCodec,
                                   IdCodec idCodec,
                                   FavouriteMovieRepository favouriteMovieRepository,
                                   UserStatusMapperHelper userStatusMapperHelper,
                                   FavouritesMapper favouritesMapper,
                                   @Value("${app.catalog.cursorPageResponse-size:20}") int defaultPageSize) {
        super(cursorCodec,
                idCodec,
                defaultPageSize,
                favouriteMovie -> idCodec.encode(favouriteMovie.getId()),
                favouriteMovies -> userStatusMapperHelper.mapWithStatuses(favouriteMovies, favouritesMapper::toOwnerView),
                favouriteMovieRepository::fetchAscending,
                favouriteMovieRepository::fetchDescending);
        this.favouriteMovieRepository = favouriteMovieRepository;
    }

    @Override
    public List<String> findTitlesStartingWith(Long userId, String searchQuery, Pageable pageable) {
        return favouriteMovieRepository.findTitlesStartingWith(userId, searchQuery, pageable);
    }
}