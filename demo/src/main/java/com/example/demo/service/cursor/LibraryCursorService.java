package com.example.demo.service.cursor;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.service.cursor.util.CursorCodec;
import com.example.demo.service.cursor.util.KeyUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LibraryCursorService extends AbstractUserCursorService<PurchasedMovie, LibrarianMovieViewDTO> {

    public LibraryCursorService(CursorCodec cursorCodec,
                                PurchasedMovieRepository purchasedMovieRepository,
                                PurchasedMapper purchasedMapper,
                                @Value("${app.catalog.page-size:20}") int defaultPageSize) {
        super(cursorCodec,
                defaultPageSize,
                purchasedMovie -> KeyUtils.encodeTimestampAndIdentifier(
                        purchasedMovie.getAddedAt(), purchasedMovie.getAbstractCatalogItemId().getMovieId()),
                purchasedMovies -> purchasedMovies.stream()
                        .map(purchasedMapper::toOwnerViewFromLibrarian)
                        .toList(),
                purchasedMovieRepository::fetchAscending,
                purchasedMovieRepository::fetchDescending);
    }
}