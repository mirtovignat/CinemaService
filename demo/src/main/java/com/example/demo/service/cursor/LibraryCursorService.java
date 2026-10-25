package com.example.demo.service.cursor;

import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.KeyCodec;
import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.util.normalizer.TitleNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LibraryCursorService extends AbstractUserCursorService<PurchasedMovie, LibrarianMovieViewDTO> {

    private final PurchasedMovieRepository purchasedMovieRepository;

    public LibraryCursorService(CursorCodec cursorCodec,
                                KeyCodec keyCodec,
                                TitleNormalizer titleNormalizer,
                                PurchasedMovieRepository purchasedMovieRepository,
                                PurchasedMapper purchasedMapper,
                                @Value("${app.catalog.cursorPageResponse-size:20}") int defaultPageSize) {
        super(cursorCodec,
                keyCodec,
                titleNormalizer,
                defaultPageSize,
                purchasedMovie -> keyCodec.encode(
                        purchasedMovie.getAddedAt(),
                        purchasedMovie.getAbstractCatalogItemId().getMovieId()
                ),
                purchasedMovies -> purchasedMovies.stream()
                        .map(purchasedMapper::toOwnerViewFromLibrarian)
                        .toList(),
                purchasedMovieRepository::fetchAscending,
                purchasedMovieRepository::fetchDescending);
        this.purchasedMovieRepository = purchasedMovieRepository;
    }

    public List<String> findTitlesStartingWith(Long userId, String query, Pageable pageable) {
        return purchasedMovieRepository.findTitlesStartingWith(userId, query, pageable);
    }
}