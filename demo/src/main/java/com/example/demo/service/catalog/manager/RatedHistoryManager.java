package com.example.demo.service.catalog.manager;

import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.mapper.entity.RatedMapper;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.entity.RatedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.util.HistoryGrouper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Component
public class RatedHistoryManager extends AbstractHistoryManager<RatedMovie, RatedMovieViewDTO> {

    private final RatedMovieRepository ratedMovieRepository;
    private final RatedMapper ratedMapper;

    public RatedHistoryManager(
            UserRepository userRepository,
            HistoryGrouper historyGrouper,
            UserMapper userMapper,
            RatedMovieRepository ratedMovieRepository,
            RatedMapper ratedMapper
    ) {
        super(userRepository, historyGrouper, userMapper);
        this.ratedMovieRepository = ratedMovieRepository;
        this.ratedMapper = ratedMapper;
    }

    @Override
    protected List<RatedMovie> fetchEntities(Long userId) {
        return ratedMovieRepository.findAllByUserId(userId);
    }

    @Override
    protected List<RatedMovieViewDTO> convertToDtos(List<RatedMovie> entities) {
        return entities.stream()
                .map(rm -> ratedMapper.toOwnerView(rm, rm.getUser().getId()))
                .toList();
    }

    @Override
    protected Function<RatedMovieViewDTO, LocalDateTime> getDateExtractor() {
        return RatedMovieViewDTO::addedAt;
    }
}