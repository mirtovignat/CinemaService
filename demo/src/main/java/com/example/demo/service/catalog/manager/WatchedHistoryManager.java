package com.example.demo.service.catalog.manager;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
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
public class WatchedHistoryManager extends AbstractHistoryManager<PurchasedMovie, LibrarianMovieViewDTO> {

    private final PurchasedMovieRepository purchasedMovieRepository;
    private final PurchasedMapper purchasedMapper;

    public WatchedHistoryManager(
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
    protected List<LibrarianMovieViewDTO> convertToDtos(List<PurchasedMovie> entities) {
        return entities.stream()
                .flatMap(p -> p.getWatchedHistory().stream()
                        .map(purchasedMapper::toLibrarianViewFromHistoryItem))
                .toList();
    }

    @Override
    protected Function<LibrarianMovieViewDTO, LocalDateTime> getDateExtractor() {
        return LibrarianMovieViewDTO::addedAt;
    }
}