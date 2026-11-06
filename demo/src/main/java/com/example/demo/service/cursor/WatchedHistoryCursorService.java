package com.example.demo.service.cursor;

import com.example.demo.config.properties.CatalogProperties;
import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.IdCodec;
import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.model.base.WatchHistoryItem;
import com.example.demo.repository.base.WatchHistoryItemRepository;
import com.example.demo.repository.projection.WatchedSuggestionProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WatchedHistoryCursorService extends AbstractUserCursorService<WatchHistoryItem, LibrarianMovieViewDTO> {

    private final WatchHistoryItemRepository watchHistoryItemRepository;

    public WatchedHistoryCursorService(CursorCodec cursorCodec,
                                       IdCodec idCodec,
                                       WatchHistoryItemRepository watchHistoryItemRepository,
                                       PurchasedMapper purchasedMapper,
                                       CatalogProperties catalogProperties) {
        super(cursorCodec,
                idCodec,
                catalogProperties.cursorPageResponseSize(),
                watchHistoryItem -> idCodec.encode(watchHistoryItem.getId()),
                watchHistoryItems -> watchHistoryItems.stream()
                        .map(purchasedMapper::toLibrarianViewFromHistoryItem)
                        .toList(),
                watchHistoryItemRepository::fetchAscending,
                watchHistoryItemRepository::fetchDescending);
        this.watchHistoryItemRepository = watchHistoryItemRepository;
    }

    public List<WatchedSuggestionProjection> findGroupedWatchedSuggestionsStartingWith(Long userId, String searchQuery, Pageable pageable) {
        return watchHistoryItemRepository.findGroupedWatchedSuggestionsStartingWith(userId, searchQuery, pageable);
    }
}