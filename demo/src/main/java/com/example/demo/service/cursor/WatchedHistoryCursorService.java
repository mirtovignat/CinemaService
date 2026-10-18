package com.example.demo.service.cursor;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.model.base.WatchHistoryItem;
import com.example.demo.repository.base.WatchHistoryItemRepository;
import com.example.demo.service.cursor.util.CursorCodec;
import com.example.demo.service.cursor.util.KeyUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WatchedHistoryCursorService extends AbstractUserCursorService<WatchHistoryItem, LibrarianMovieViewDTO> {

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
                        .toList(),
                watchHistoryItemRepository::fetchAscending,
                watchHistoryItemRepository::fetchDescending);
    }
}