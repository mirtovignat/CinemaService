package com.example.demo.service.catalog;

import com.example.demo.dto.base.PageCursor;
import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.base.WatchHistoryItem;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.model.entity.User;
import com.example.demo.repository.base.WatchHistoryItemRepository;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.service.cursor.AbstractCursorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class WatchedHistoryService extends AbstractCursorService<WatchHistoryItem, LibrarianMovieViewDTO> {

    private final PurchasedMovieRepository purchasedMovieRepository;
    private final WatchHistoryItemRepository watchHistoryItemRepository;
    private final UserRepository userRepository;

    public WatchedHistoryService(PurchasedMovieRepository purchasedMovieRepository,
                                 WatchHistoryItemRepository watchHistoryItemRepository,
                                 UserRepository userRepository,
                                 UserMapper userMapper,
                                 PurchasedMapper purchasedMapper,
                                 @Value("${app.catalog.page-size:20}") int defaultPageSize) {
        super(userRepository,
                userMapper,
                entities -> entities.stream()
                        .map(purchasedMapper::toLibrarianViewFromHistoryItem)
                        .toList(),
                WatchedHistoryService::extractCursor,
                defaultPageSize);
        this.purchasedMovieRepository = purchasedMovieRepository;
        this.watchHistoryItemRepository = watchHistoryItemRepository;
        this.userRepository = userRepository;
    }

    private static PageCursor extractCursor(WatchHistoryItem item) {
        return PageCursor.builder()
                .addedAt(item.getWatchedAt().toInstant(ZoneOffset.UTC))
                .entityId(item.getId())
                .build();
    }

    @Override
    protected Slice<WatchHistoryItem> fetchSlice(Long userId,
                                                 PageCursor pageCursor,
                                                 boolean isAscending,
                                                 Pageable pageable) {
        if (pageCursor == null) {
            return isAscending
                    ? watchHistoryItemRepository.findPageAscFirst(userId, pageable)
                    : watchHistoryItemRepository.findPageDescFirst(userId, pageable);
        }
        return isAscending
                ? watchHistoryItemRepository.findPageAscAfter(userId, pageCursor.addedAtUtc(), pageCursor.entityId(), pageable)
                : watchHistoryItemRepository.findPageDescAfter(userId, pageCursor.addedAtUtc(), pageCursor.entityId(), pageable);
    }

    @Transactional
    public void watch(Long movieId, Long userId) {
        PurchasedMovie purchasedMovie = purchasedMovieRepository
                .findByMovieIdAndUserId(movieId, userId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.MOVIE_NOT_PURCHASED));
        purchasedMovie.addWatchHistory(LocalDateTime.now(ZoneOffset.UTC));
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
        WatchHistoryItem watchHistoryItem = watchHistoryItemRepository
                .findByIdAndUserId(historyId, userId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ENTITY_NOT_FOUND));
        watchHistoryItemRepository.delete(watchHistoryItem);
    }
}