package com.example.demo.service.catalog.cursor;

import com.example.demo.dto.base.UserStatus;
import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.mapper.entity.FavouritesMapper;
import com.example.demo.model.entity.FavouriteMovie;
import com.example.demo.repository.entity.FavouriteMovieRepository;
import com.example.demo.service.cursor.AbstractCursorService;
import com.example.demo.service.cursor.CursorCodec;
import com.example.demo.service.cursor.KeyUtils;
import com.example.demo.service.cursor.context.UserCursorContext;
import com.example.demo.service.linked_collection.UserStatusResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class FavouritesCursorService
        extends AbstractCursorService<FavouriteMovie, FavouriteMovieViewDTO, UserCursorContext> {

    private final FavouriteMovieRepository favouriteMovieRepository;

    public FavouritesCursorService(CursorCodec cursorCodec,
                                   FavouriteMovieRepository favouriteMovieRepository,
                                   UserStatusResolver userStatusResolver,
                                   FavouritesMapper favouritesMapper,
                                   @Value("${app.catalog.page-size:20}") int defaultPageSize) {
        super(cursorCodec,
                defaultPageSize,
                favouriteMovie -> KeyUtils.encodeTimestampAndIdentifier(
                        favouriteMovie.getAddedAt(), favouriteMovie.getAbstractCatalogItemId().getMovieId()),
                favouriteMovies -> mapWithStatuses(favouriteMovies, userStatusResolver, favouritesMapper));
        this.favouriteMovieRepository = favouriteMovieRepository;
    }

    @Override
    protected Slice<FavouriteMovie> fetchSlice(String cursorValue,
                                               String sortDirection,
                                               PageRequest pageRequest,
                                               UserCursorContext context) {
        Long userId = context.userId();
        boolean hasCursor = cursorValue != null;
        LocalDateTime cursorAddedAt = hasCursor ? KeyUtils.decodeTimestamp(cursorValue) : null;
        Long cursorMovieId = hasCursor ? KeyUtils.decodeIdentifier(cursorValue) : null;
        return "asc".equals(sortDirection)
                ? favouriteMovieRepository.fetchAscending(userId, hasCursor, cursorAddedAt, cursorMovieId, pageRequest)
                : favouriteMovieRepository.fetchDescending(userId, hasCursor, cursorAddedAt, cursorMovieId, pageRequest);
    }

    private static List<FavouriteMovieViewDTO> mapWithStatuses(List<FavouriteMovie> favouriteMovies,
                                                               UserStatusResolver userStatusResolver,
                                                               FavouritesMapper favouritesMapper) {
        if (favouriteMovies.isEmpty()) {
            return List.of();
        }
        Long userId = favouriteMovies.get(0).getAbstractCatalogItemId().getUserId();
        List<Long> movieIds = favouriteMovies.stream()
                .map(favouriteMovie -> favouriteMovie.getAbstractCatalogItemId().getMovieId())
                .toList();
        Map<Long, UserStatus> userStatuses = userStatusResolver.resolveAll(userId, movieIds);
        return favouriteMovies.stream()
                .map(favouriteMovie -> favouritesMapper.toOwnerView(
                        favouriteMovie,
                        userStatuses.getOrDefault(favouriteMovie.getAbstractCatalogItemId().getMovieId(),
                                UserStatus.builder().build())))
                .toList();
    }
}