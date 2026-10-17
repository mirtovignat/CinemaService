package com.example.demo.service.catalog.cursor;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.model.base.WatchHistoryItem;
import com.example.demo.repository.base.WatchHistoryItemRepository;
import com.example.demo.service.cursor.AbstractCursorService;
import com.example.demo.service.cursor.CursorCodec;
import com.example.demo.service.cursor.KeyUtils;
import com.example.demo.service.cursor.context.UserCursorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WatchedHistoryCursorService
        extends AbstractCursorService<WatchHistoryItem, LibrarianMovieViewDTO, UserCursorContext> {

    private final WatchHistoryItemRepository watchHistoryItemRepository;

    public WatchedHistoryCursorService(CursorCodec cursorCodec,
                                       WatchHistoryItemRepository watchHistoryItemRepository,
                                       PurchasedMapper purchasedMapper,
                                       @Value("${app.catalog.page-size:20}") int defaultPageSize) {
        super(cursorCodec,
                defaultPageSize,
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
                                                 UserCursorContext context) {
        Long userId = context.userId();
        boolean hasCursor = cursorValue != null;
        LocalDateTime cursorWatchedAt = hasCursor ? KeyUtils.decodeTimestamp(cursorValue) : null;
        Long cursorIdentifier = hasCursor ? KeyUtils.decodeIdentifier(cursorValue) : null;
        return "asc".equals(sortDirection)
                ? watchHistoryItemRepository.fetchAscending(userId, hasCursor, cursorWatchedAt, cursorIdentifier, pageRequest)
                : watchHistoryItemRepository.fetchDescending(userId, hasCursor, cursorWatchedAt, cursorIdentifier, pageRequest);
    }
}