package com.example.demo.service.catalog;

import com.example.demo.dto.base.PageCursor;
import com.example.demo.dto.base.UserStatus;
import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.mapper.entity.FavouritesMapper;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.entity.FavouriteMovie;
import com.example.demo.repository.entity.FavouriteMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.service.cursor.AbstractCursorService;
import com.example.demo.service.linked_collection.UserStatusResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
public class FavouritesCatalogService extends AbstractCursorService<FavouriteMovie, FavouriteMovieViewDTO> {

    private final FavouriteMovieRepository favouriteMovieRepository;

    public FavouritesCatalogService(FavouriteMovieRepository favouriteMovieRepository,
                                    UserRepository userRepository,
                                    UserMapper userMapper,
                                    UserStatusResolver userStatusResolver,
                                    FavouritesMapper favouritesMapper,
                                    @Value("${app.catalog.page-size:20}") int defaultPageSize) {
        super(userRepository,
                userMapper,
                entities -> mapWithStatuses(entities, userStatusResolver, favouritesMapper),
                FavouritesCatalogService::extractCursor,
                defaultPageSize);
        this.favouriteMovieRepository = favouriteMovieRepository;
    }

    private static List<FavouriteMovieViewDTO> mapWithStatuses(List<FavouriteMovie> items,
                                                               UserStatusResolver resolver,
                                                               FavouritesMapper mapper) {
        if (items.isEmpty()) return List.of();
        Long userId = items.get(0).getId().getUserId();
        List<Long> movieIds = items.stream()
                .map(item -> item.getId().getMovieId())
                .toList();
        Map<Long, UserStatus> statuses = resolver.resolveAll(userId, movieIds);
        return items.stream()
                .map(item -> mapper.toOwnerView(
                        item,
                        statuses.getOrDefault(item.getId().getMovieId(),
                                UserStatus.builder().build())))
                .toList();
    }

    private static PageCursor extractCursor(FavouriteMovie item) {
        return PageCursor.builder()
                .addedAt(item.getAddedAt().toInstant(ZoneOffset.UTC))
                .entityId(item.getId().getMovieId())
                .build();
    }

    @Override
    protected Slice<FavouriteMovie> fetchSlice(Long userId,
                                               PageCursor pageCursor,
                                               boolean isAscending,
                                               Pageable pageable) {
        if (pageCursor == null) {
            return isAscending
                    ? favouriteMovieRepository.findPageAscFirst(userId, pageable)
                    : favouriteMovieRepository.findPageDescFirst(userId, pageable);
        }
        return isAscending
                ? favouriteMovieRepository.findPageAscAfter(userId, pageCursor.addedAtUtc(), pageCursor.entityId(), pageable)
                : favouriteMovieRepository.findPageDescAfter(userId, pageCursor.addedAtUtc(), pageCursor.entityId(), pageable);
    }
}