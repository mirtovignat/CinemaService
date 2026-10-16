package com.example.demo.service.catalog;

import com.example.demo.dto.base.PageCursor;
import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.mapper.entity.RatedMapper;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.entity.RatedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.service.cursor.AbstractCursorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
public class RatedCatalogService extends AbstractCursorService<RatedMovie, RatedMovieViewDTO> {

    private final RatedMovieRepository ratedMovieRepository;

    public RatedCatalogService(RatedMovieRepository ratedMovieRepository,
                               UserRepository userRepository,
                               UserMapper userMapper,
                               RatedMapper ratedMapper,
                               @Value("${app.catalog.page-size:20}") int defaultPageSize) {
        super(userRepository,
                userMapper,
                entities -> entities.stream()
                        .map(ratedMapper::toOwnerView)
                        .toList(),
                RatedCatalogService::extractCursor,
                defaultPageSize);
        this.ratedMovieRepository = ratedMovieRepository;
    }

    private static PageCursor extractCursor(RatedMovie ratedMovie) {
        return PageCursor.builder()
                .addedAt(ratedMovie.getAddedAt())
                .entityId(ratedMovie.getId().getMovieId())
                .build();
    }

    @Override
    protected Slice<RatedMovie> fetchSlice(Long userId,
                                           PageCursor pageCursor,
                                           boolean isAscending,
                                           Pageable pageable) {
        if (pageCursor == null) {
            return isAscending
                    ? ratedMovieRepository.findRatedPageAscFirst(userId, pageable)
                    : ratedMovieRepository.findRatedPageDescFirst(userId, pageable);
        }
        return isAscending
                ? ratedMovieRepository.findRatedPageAscAfter(userId, pageCursor.addedAt(), pageCursor.entityId(), pageable)
                : ratedMovieRepository.findRatedPageDescAfter(userId, pageCursor.addedAt(), pageCursor.entityId(), pageable);
    }
}