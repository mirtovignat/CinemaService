package com.example.demo.service.cursor;

import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.KeyCodec;
import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.mapper.entity.RatedMapper;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.entity.RatedMovieRepository;
import com.example.demo.util.normalizer.TitleNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RatedCursorService extends AbstractUserCursorService<RatedMovie, RatedMovieViewDTO> {

    private final RatedMovieRepository ratedMovieRepository;

    public RatedCursorService(CursorCodec cursorCodec,
                              KeyCodec keyCodec,
                              TitleNormalizer titleNormalizer,
                              RatedMovieRepository ratedMovieRepository,
                              RatedMapper ratedMapper,
                              @Value("${app.catalog.cursorPageResponse-size:20}") int defaultPageSize) {
        super(cursorCodec,
                keyCodec,
                titleNormalizer,
                defaultPageSize,
                ratedMovie -> keyCodec.encode(
                        ratedMovie.getAddedAt(),
                        ratedMovie.getAbstractCatalogItemId()
                                .getMovieId()
                ),
                ratedMovies -> ratedMovies.stream()
                        .map(ratedMapper::toOwnerView)
                        .toList(),
                ratedMovieRepository::fetchAscending,
                ratedMovieRepository::fetchDescending);
        this.ratedMovieRepository = ratedMovieRepository;
    }

    public List<String> findTitlesStartingWith(Long userId, String query, Pageable pageable) {
        return ratedMovieRepository.findTitlesStartingWith(userId, query, pageable);
    }
}