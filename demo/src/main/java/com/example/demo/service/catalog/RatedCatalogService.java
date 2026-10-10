package com.example.demo.service.catalog;

import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.service.catalog.manager.RatedHistoryManager;
import org.springframework.stereotype.Service;

@Service
public class RatedCatalogService extends AbstractGroupedHistoryService<RatedMovieViewDTO> {

    public RatedCatalogService(RatedHistoryManager ratedHistoryManager) {
        super(ratedHistoryManager);
    }
}