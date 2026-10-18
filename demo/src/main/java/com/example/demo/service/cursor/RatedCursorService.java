package com.example.demo.service.cursor;

import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.mapper.entity.RatedMapper;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.entity.RatedMovieRepository;
import com.example.demo.service.cursor.util.CursorCodec;
import com.example.demo.service.cursor.util.KeyUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RatedCursorService extends AbstractUserCursorService<RatedMovie, RatedMovieViewDTO> {

    public RatedCursorService(CursorCodec cursorCodec,
                              RatedMovieRepository ratedMovieRepository,
                              RatedMapper ratedMapper,
                              @Value("${app.catalog.page-size:20}") int defaultPageSize) {
        super(cursorCodec,
                defaultPageSize,
                ratedMovie -> KeyUtils.encodeTimestampAndIdentifier(
                        ratedMovie.getAddedAt(), ratedMovie.getAbstractCatalogItemId().getMovieId()),
                ratedMovies -> ratedMovies.stream()
                        .map(ratedMapper::toOwnerView)
                        .toList(),
                ratedMovieRepository::fetchAscending,
                ratedMovieRepository::fetchDescending);
    }
}