package com.example.demo.service.catalog;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.base.WatchHistoryItem;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.model.entity.User;
import com.example.demo.repository.base.WatchHistoryItemRepository;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@AllArgsConstructor
@Service
public class WatchedHistoryService {

    private final PurchasedMovieRepository purchasedMovieRepository;
    private final WatchHistoryItemRepository watchHistoryItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long watch(Long movieId, Long userId) {
        PurchasedMovie purchasedMovie = purchasedMovieRepository
                .findByMovieIdAndUserId(movieId, userId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.MOVIE_NOT_PURCHASED, movieId));

        int previousSize = purchasedMovie.getWatchedHistory().size();
        purchasedMovie.addWatchHistory(LocalDateTime.now(ZoneOffset.UTC));
        purchasedMovieRepository.saveAndFlush(purchasedMovie);

        if (purchasedMovie.getWatchedHistory().size() > previousSize) {
            return purchasedMovie.getWatchedHistory()
                    .get(purchasedMovie.getWatchedHistory().size() - 1)
                    .getId();
        }
        return null;
    }

    @Transactional
    public boolean ensurePurchased(Long movieId, Long userId) {
        return purchasedMovieRepository.findByMovieIdAndUserId(movieId, userId).isPresent();
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

    public boolean isHistorySaved(Long userId) {
        return userRepository.findSaveHistoryById(userId);
    }

    @Transactional
    public void removeHistoryItem(Long userId, Long historyId) {
        WatchHistoryItem watchHistoryItem = watchHistoryItemRepository
                .findByIdAndUserId(historyId, userId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ENTITY_NOT_FOUND));
        watchHistoryItemRepository.delete(watchHistoryItem);
    }
}