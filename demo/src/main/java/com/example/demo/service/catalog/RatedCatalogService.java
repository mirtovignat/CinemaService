// demo/src/main/java/com/example/demo/service/catalog/RatedCatalogService.java
package com.example.demo.service.catalog;

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
                ratedMovie -> ratedMovie.getId().getMovieId(),
                defaultPageSize);
        this.ratedMovieRepository = ratedMovieRepository;
    }

    @Override
    protected Slice<RatedMovie> fetchSlice(Long userId,
                                           Long cursorValue,
                                           boolean isAscending,
                                           Pageable pageable) {
        return isAscending
                ? ratedMovieRepository.findRatedPageAsc(userId, cursorValue, pageable)
                : ratedMovieRepository.findRatedPageDesc(userId, cursorValue, pageable);
    }
}