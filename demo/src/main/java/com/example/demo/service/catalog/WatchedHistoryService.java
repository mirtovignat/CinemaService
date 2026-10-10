package com.example.demo.service.catalog;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.base.WatchHistoryItem;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.model.entity.User;
import com.example.demo.repository.base.WatchHistoryItemRepository;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.service.catalog.manager.WatchedHistoryManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WatchedHistoryService extends AbstractGroupedHistoryService<LibrarianMovieViewDTO> {

    private final PurchasedMovieRepository purchasedMovieRepository;
    private final WatchHistoryItemRepository watchHistoryItemRepository;
    private final UserRepository userRepository;

    public WatchedHistoryService(WatchedHistoryManager watchedHistoryManager,
                                 PurchasedMovieRepository purchasedMovieRepository,
                                 WatchHistoryItemRepository watchHistoryItemRepository,
                                 UserRepository userRepository) {
        super(watchedHistoryManager);
        this.purchasedMovieRepository = purchasedMovieRepository;
        this.watchHistoryItemRepository = watchHistoryItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void watch(Long movieId, Long userId) {
        PurchasedMovie purchasedMovie = purchasedMovieRepository
                .findByMovieIdAndUserId(movieId, userId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.MOVIE_NOT_PURCHASED));
        purchasedMovie.addWatchHistory(LocalDateTime.now());
        purchasedMovieRepository.saveAndFlush(purchasedMovie);
    }

    @Transactional
    public void clear(Long userId) {
        List<PurchasedMovie> purchases = purchasedMovieRepository.findAllByUserId(userId);
        if (purchases.isEmpty()) {
            throw BusinessException.of(ErrorCode.EMPTY_HISTORY);
        }
        purchases.forEach(PurchasedMovie::clearHistory);
        purchasedMovieRepository.saveAll(purchases);
    }

    @Transactional
    public void setWatchedHistory(Long userId, boolean save) {
        User user = userRepository.findByIdOrThrow(userId);
        user.setSaveHistory(save);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean isHistorySaved(Long userId) {
        return userRepository.findSaveHistoryById(userId);
    }

    @Transactional
    public void removeHistoryItem(Long userId, Long historyId) {
        WatchHistoryItem item = watchHistoryItemRepository
                .findByIdAndUserId(historyId, userId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ENTITY_NOT_FOUND));
        watchHistoryItemRepository.delete(item);
    }
}