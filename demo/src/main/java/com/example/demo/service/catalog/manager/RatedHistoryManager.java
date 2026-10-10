package com.example.demo.service.catalog.manager;

import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.mapper.entity.RatedMapper;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.entity.RatedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.util.HistoryGrouper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RatedHistoryManager
        extends AbstractCatalogItemHistoryManager<RatedMovie, RatedMovieViewDTO> {

    private final RatedMapper ratedMapper;

    public RatedHistoryManager(UserRepository userRepository,
                               HistoryGrouper historyGrouper,
                               UserMapper userMapper,
                               RatedMovieRepository ratedMovieRepository,
                               RatedMapper ratedMapper) {
        super(userRepository, historyGrouper, userMapper, ratedMovieRepository,
                RatedMovieViewDTO::addedAt);
        this.ratedMapper = ratedMapper;
    }

    @Override
    protected List<RatedMovieViewDTO> convertToDtos(List<RatedMovie> entities) {
        return entities.stream()
                .map(ratedMovie -> ratedMapper.toOwnerView(ratedMovie, ratedMovie.getUser().getId()))
                .toList();
    }
}