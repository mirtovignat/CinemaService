package com.example.demo.service.catalog;

import com.example.demo.dto.catalog.HistoricalMovieViewDTO;
import com.example.demo.service.catalog.manager.PurchaseHistoryManager;
import org.springframework.stereotype.Service;

@Service
public class HistoryService extends AbstractGroupedHistoryService<HistoricalMovieViewDTO> {

    public HistoryService(PurchaseHistoryManager purchaseHistoryManager) {
        super(purchaseHistoryManager);
    }
}