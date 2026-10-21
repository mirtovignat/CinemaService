package com.example.demo.service.cursor;

import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.KeyCodec;
import com.example.demo.dto.catalog.HistoricalMovieViewDTO;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HistoryCursorService extends AbstractUserCursorService<PurchasedMovie, HistoricalMovieViewDTO> {

    public HistoryCursorService(CursorCodec cursorCodec,
                                KeyCodec keyCodec,
                                PurchasedMovieRepository purchasedMovieRepository,
                                PurchasedMapper purchasedMapper,
                                @Value("${app.catalog.cursorPageResponse-size:20}") int defaultPageSize) {
        super(cursorCodec,
                keyCodec,
                defaultPageSize,
                purchasedMovie -> keyCodec.encode(
                        purchasedMovie.getAddedAt(),
                        purchasedMovie.getAbstractCatalogItemId().getMovieId()),
                purchasedMovies -> purchasedMovies.stream()
                        .map(purchasedMapper::toOwnerViewFromHistorical)
                        .toList(),
                purchasedMovieRepository::fetchAscending,
                purchasedMovieRepository::fetchDescending);
    }
}