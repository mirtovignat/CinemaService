package com.example.demo.service.catalog.cursor;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.model.base.WatchHistoryItem;
import com.example.demo.repository.base.WatchHistoryItemRepository;
import com.example.demo.service.cursor.AbstractCursorService;
import com.example.demo.service.cursor.CursorCodec;
import com.example.demo.service.cursor.KeyUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WatchedHistoryCursorService
        extends AbstractCursorService<WatchHistoryItem, LibrarianMovieViewDTO> {

    private final WatchHistoryItemRepository watchHistoryItemRepository;

    public WatchedHistoryCursorService(CursorCodec cursorCodec,
                                       WatchHistoryItemRepository watchHistoryItemRepository,
                                       PurchasedMapper purchasedMapper,
                                       @Value("${app.catalog.page-size:20}") int defaultPageSize,
                                       @Value("${app.catalog.max-page-size:100}") int maximumPageSize) {
        super(cursorCodec,
                defaultPageSize,
                maximumPageSize,
                watchHistoryItem -> KeyUtils.encodeTimestampAndIdentifier(
                        watchHistoryItem.getWatchedAt(), watchHistoryItem.getId()),
                watchHistoryItems -> watchHistoryItems.stream()
                        .map(purchasedMapper::toLibrarianViewFromHistoryItem)
                        .toList());
        this.watchHistoryItemRepository = watchHistoryItemRepository;
    }

    @Override
    protected Slice<WatchHistoryItem> fetchSlice(String cursorValue,
                                                 String sortDirection,
                                                 PageRequest pageRequest,
                                                 Long... contextValues) {
        Long userId = contextValues[0];
        boolean hasCursor = cursorValue != null;
        LocalDateTime cursorWatchedAt = hasCursor ? KeyUtils.decodeTimestamp(cursorValue) : null;
        Long cursorIdentifier = hasCursor ? KeyUtils.decodeIdentifier(cursorValue) : null;
        return "asc".equals(sortDirection)
                ? watchHistoryItemRepository.fetchAscending(userId, hasCursor, cursorWatchedAt, cursorIdentifier, pageRequest)
                : watchHistoryItemRepository.fetchDescending(userId, hasCursor, cursorWatchedAt, cursorIdentifier, pageRequest);
    }
}