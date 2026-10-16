package com.example.demo.service.catalog;

import com.example.demo.dto.base.PageCursor;
import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.service.cursor.AbstractCursorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

@Service
public class LibraryService extends AbstractCursorService<PurchasedMovie, LibrarianMovieViewDTO> {

    private final PurchasedMovieRepository purchasedMovieRepository;

    public LibraryService(PurchasedMovieRepository purchasedMovieRepository,
                          UserRepository userRepository,
                          UserMapper userMapper,
                          PurchasedMapper purchasedMapper,
                          @Value("${app.catalog.page-size:20}") int defaultPageSize) {
        super(userRepository,
                userMapper,
                entities -> entities.stream()
                        .map(purchasedMapper::toOwnerViewFromLibrarian)
                        .toList(),
                LibraryService::extractCursor,
                defaultPageSize);
        this.purchasedMovieRepository = purchasedMovieRepository;
    }

    private static PageCursor extractCursor(PurchasedMovie purchasedMovie) {
        return PageCursor.builder()
                .addedAt(purchasedMovie.getAddedAt().toInstant(ZoneOffset.UTC))
                .entityId(purchasedMovie.getId().getMovieId())
                .build();
    }

    @Override
    protected Slice<PurchasedMovie> fetchSlice(Long userId,
                                               PageCursor pageCursor,
                                               boolean isAscending,
                                               Pageable pageable) {
        if (pageCursor == null) {
            return isAscending
                    ? purchasedMovieRepository.findPageAscFirst(userId, pageable)
                    : purchasedMovieRepository.findPageDescFirst(userId, pageable);
        }
        return isAscending
                ? purchasedMovieRepository.findPageAscAfter(userId, pageCursor.addedAtUtc(), pageCursor.entityId(), pageable)
                : purchasedMovieRepository.findPageDescAfter(userId, pageCursor.addedAtUtc(), pageCursor.entityId(), pageable);
    }
}