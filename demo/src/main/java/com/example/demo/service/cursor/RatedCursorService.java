package com.example.demo.service.cursor;

import com.example.demo.config.properties.CatalogProperties;
import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.IdCodec;
import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.mapper.entity.RatedMapper;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.entity.RatedMovieRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RatedCursorService extends AbstractUserCursorService<RatedMovie, RatedMovieViewDTO>
        implements SuggestableCursorService {

    private final RatedMovieRepository ratedMovieRepository;

    public RatedCursorService(CursorCodec cursorCodec,
                              IdCodec idCodec,
                              RatedMovieRepository ratedMovieRepository,
                              RatedMapper ratedMapper,
                              CatalogProperties catalogProperties) {
        super(cursorCodec,
                idCodec,
                catalogProperties.cursorPageResponseSize(),
                ratedMovie -> idCodec.encode(ratedMovie.getId()),
                ratedMovies -> ratedMovies.stream().map(ratedMapper::toOwnerView).toList(),
                ratedMovieRepository::fetchAscending,
                ratedMovieRepository::fetchDescending);
        this.ratedMovieRepository = ratedMovieRepository;
    }

    @Override
    public List<String> findTitlesStartingWith(Long userId, String searchQuery, Pageable pageable) {
        return ratedMovieRepository.findTitlesStartingWith(userId, searchQuery, pageable);
    }
}