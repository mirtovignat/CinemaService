package com.example.demo.service.catalog.cursor;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.service.cursor.AbstractCursorService;
import com.example.demo.service.cursor.CursorCodec;
import com.example.demo.service.cursor.KeyUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LibraryCursorService
        extends AbstractCursorService<PurchasedMovie, LibrarianMovieViewDTO> {

    private final PurchasedMovieRepository purchasedMovieRepository;

    public LibraryCursorService(CursorCodec cursorCodec,
                                PurchasedMovieRepository purchasedMovieRepository,
                                PurchasedMapper purchasedMapper,
                                @Value("${app.catalog.page-size:20}") int defaultPageSize,
                                @Value("${app.catalog.max-page-size:100}") int maximumPageSize) {
        super(cursorCodec,
                defaultPageSize,
                maximumPageSize,
                purchasedMovie -> KeyUtils.encodeTimestampAndIdentifier(
                        purchasedMovie.getAddedAt(), purchasedMovie.getId().getMovieId()),
                purchasedMovies -> purchasedMovies.stream()
                        .map(purchasedMapper::toOwnerViewFromLibrarian)
                        .toList());
        this.purchasedMovieRepository = purchasedMovieRepository;
    }

    @Override
    protected Slice<PurchasedMovie> fetchSlice(String cursorValue,
                                               String sortDirection,
                                               PageRequest pageRequest,
                                               Long... contextValues) {
        Long userId = contextValues[0];
        boolean hasCursor = cursorValue != null;
        LocalDateTime cursorAddedAt = hasCursor ? KeyUtils.decodeTimestamp(cursorValue) : null;
        Long cursorMovieId = hasCursor ? KeyUtils.decodeIdentifier(cursorValue) : null;
        return "asc".equals(sortDirection)
                ? purchasedMovieRepository.fetchAscending(userId, hasCursor, cursorAddedAt, cursorMovieId, pageRequest)
                : purchasedMovieRepository.fetchDescending(userId, hasCursor, cursorAddedAt, cursorMovieId, pageRequest);
    }
}