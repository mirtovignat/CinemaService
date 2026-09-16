package com.example.demo.service.catalog.manager;

import com.example.demo.dto.catalog.HistoricalMovieViewDTO;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.util.HistoryGrouper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Component
public class PurchaseHistoryManager extends AbstractHistoryManager<PurchasedMovie, HistoricalMovieViewDTO> {

    private final PurchasedMovieRepository purchasedMovieRepository;
    private final PurchasedMapper purchasedMapper;

    public PurchaseHistoryManager(
            UserRepository userRepository,
            HistoryGrouper historyGrouper,
            UserMapper userMapper,
            PurchasedMovieRepository purchasedMovieRepository,
            PurchasedMapper purchasedMapper
    ) {
        super(userRepository, historyGrouper, userMapper);
        this.purchasedMovieRepository = purchasedMovieRepository;
        this.purchasedMapper = purchasedMapper;
    }

    @Override
    protected List<PurchasedMovie> fetchEntities(Long userId) {
        return purchasedMovieRepository.findAllByUserId(userId);
    }

    @Override
    protected List<HistoricalMovieViewDTO> convertToDtos(List<PurchasedMovie> entities) {
        return entities.stream()
                .map(purchasedMapper::toOwnerViewFromHistorical)
                .toList();
    }

    @Override
    protected Function<HistoricalMovieViewDTO, LocalDateTime> getDateExtractor() {
        return HistoricalMovieViewDTO::addedAt;
    }
}