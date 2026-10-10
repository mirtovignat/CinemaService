package com.example.demo.service.catalog.manager;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.util.HistoryGrouper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WatchedHistoryManager
        extends AbstractCatalogItemHistoryManager<PurchasedMovie, LibrarianMovieViewDTO> {

    private final PurchasedMapper purchasedMapper;

    public WatchedHistoryManager(UserRepository userRepository,
                                 HistoryGrouper historyGrouper,
                                 UserMapper userMapper,
                                 PurchasedMovieRepository purchasedMovieRepository,
                                 PurchasedMapper purchasedMapper) {
        super(userRepository, historyGrouper, userMapper, purchasedMovieRepository,
                LibrarianMovieViewDTO::addedAt);
        this.purchasedMapper = purchasedMapper;
    }

    @Override
    protected List<LibrarianMovieViewDTO> convertToDtos(List<PurchasedMovie> entities) {
        return entities.stream()
                .flatMap(purchasedMovie -> purchasedMovie.getWatchedHistory().stream()
                        .map(purchasedMapper::toLibrarianViewFromHistoryItem))
                .toList();
    }
}