package com.example.demo.service.cursor;

import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.KeyCodec;
import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.dto.catalog.WatchedSuggestionDTO;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.model.base.WatchHistoryItem;
import com.example.demo.repository.base.WatchHistoryItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WatchedHistoryCursorService extends AbstractUserCursorService<WatchHistoryItem, LibrarianMovieViewDTO> {

    private final WatchHistoryItemRepository watchHistoryItemRepository;

    public WatchedHistoryCursorService(CursorCodec cursorCodec,
                                       KeyCodec keyCodec,
                                       WatchHistoryItemRepository watchHistoryItemRepository,
                                       PurchasedMapper purchasedMapper,
                                       @Value("${app.catalog.cursorPageResponse-size:20}") int defaultPageSize) {
        super(cursorCodec,
                keyCodec,
                defaultPageSize,
                watchHistoryItem -> keyCodec.encode(
                        watchHistoryItem.getWatchedAt(),
                        watchHistoryItem.getId()),
                watchHistoryItems -> watchHistoryItems.stream()
                        .map(purchasedMapper::toLibrarianViewFromHistoryItem)
                        .toList(),
                watchHistoryItemRepository::fetchAscending,
                watchHistoryItemRepository::fetchDescending);
        this.watchHistoryItemRepository = watchHistoryItemRepository;
    }

    public List<WatchedSuggestionDTO> findGroupedWatchedSuggestionsStartingWith(Long userId,
                                                                                String prefix,
                                                                                Pageable pageable) {
        return watchHistoryItemRepository.findGroupedWatchedSuggestionsStartingWith(userId, prefix, pageable);
    }

}