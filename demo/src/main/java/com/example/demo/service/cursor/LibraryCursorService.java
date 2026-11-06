package com.example.demo.service.cursor;

import com.example.demo.config.properties.CatalogProperties;
import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.IdCodec;
import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LibraryCursorService extends AbstractUserCursorService<PurchasedMovie, LibrarianMovieViewDTO>
        implements SuggestableCursorService {

    private final PurchasedMovieRepository purchasedMovieRepository;

    public LibraryCursorService(CursorCodec cursorCodec,
                                IdCodec idCodec,
                                PurchasedMovieRepository purchasedMovieRepository,
                                PurchasedMapper purchasedMapper,
                                CatalogProperties catalogProperties) {
        super(cursorCodec,
                idCodec,
                catalogProperties.cursorPageResponseSize(),
                purchasedMovie -> idCodec.encode(purchasedMovie.getId()),
                purchasedMovies -> purchasedMovies.stream().map(purchasedMapper::toOwnerViewFromLibrarian).toList(),
                purchasedMovieRepository::fetchAscending,
                purchasedMovieRepository::fetchDescending);
        this.purchasedMovieRepository = purchasedMovieRepository;
    }

    @Override
    public List<String> findTitlesStartingWith(Long userId, String searchQuery, Pageable pageable) {
        return purchasedMovieRepository.findTitlesStartingWith(userId, searchQuery, pageable);
    }
}