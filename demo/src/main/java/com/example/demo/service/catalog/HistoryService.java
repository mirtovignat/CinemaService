package com.example.demo.service.catalog;

import com.example.demo.dto.catalog.HistoricalMovieViewDTO;
import com.example.demo.dto.response.GroupedHistoryResponse;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.service.catalog.manager.PurchaseHistoryManager;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class HistoryService extends AbstractCatalogService<PurchasedMovie, HistoricalMovieViewDTO> {

    private final PurchasedMapper purchasedMapper;
    private final PurchaseHistoryManager purchaseHistoryManager;

    protected HistoryService(PurchasedMovieRepository purchasedMovieRepository,
                             PurchasedMapper purchasedMapper,
                             PurchaseHistoryManager purchaseHistoryManager) {
        super(purchasedMovieRepository);
        this.purchasedMapper = purchasedMapper;
        this.purchaseHistoryManager = purchaseHistoryManager;
    }

    @Override
    protected Function<PurchasedMovie, HistoricalMovieViewDTO> getMapper() {
        return purchasedMapper::toOwnerViewFromHistorical;
    }

    public GroupedHistoryResponse<HistoricalMovieViewDTO> getGroupedHistory(Long userId) {
        return purchaseHistoryManager.getGroupedHistory(userId);
    }
}