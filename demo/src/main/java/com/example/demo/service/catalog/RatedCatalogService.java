package com.example.demo.service.catalog;

import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.dto.response.GroupedHistoryResponse;
import com.example.demo.mapper.entity.RatedMapper;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.entity.RatedMovieRepository;
import com.example.demo.service.catalog.manager.RatedHistoryManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

@Service
public class RatedCatalogService extends AbstractCatalogService<RatedMovie, RatedMovieViewDTO> {

    private final RatedMapper ratedMapper;
    private final RatedHistoryManager ratedHistoryManager;

    public RatedCatalogService(RatedMovieRepository ratedMovieRepository,
                               RatedMapper ratedMapper,
                               RatedHistoryManager ratedHistoryManager) {
        super(ratedMovieRepository);
        this.ratedMapper = ratedMapper;
        this.ratedHistoryManager = ratedHistoryManager;
    }

    @Override
    public List<RatedMovieViewDTO> getCatalog(Long userId) {
        return catalogRepository.findAllByUserId(userId).stream()
                .map(ratedMovie -> ratedMapper.toOwnerView(ratedMovie, userId))
                .toList();
    }

    @Override
    protected Function<RatedMovie, RatedMovieViewDTO> getMapper() {
        return ratedMovie -> ratedMapper.toOwnerView(ratedMovie, null);
    }

    public GroupedHistoryResponse<RatedMovieViewDTO> getGroupedRatedHistory(Long userId) {
        return ratedHistoryManager.getGroupedHistory(userId);
    }
}