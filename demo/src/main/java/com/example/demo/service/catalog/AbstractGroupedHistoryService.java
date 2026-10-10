package com.example.demo.service.catalog;

import com.example.demo.dto.response.GroupedHistoryResponse;
import com.example.demo.service.catalog.manager.AbstractHistoryManager;

public abstract class AbstractGroupedHistoryService<DTO> {

    private final AbstractHistoryManager<?, DTO> abstractHistoryManager;

    protected AbstractGroupedHistoryService(AbstractHistoryManager<?, DTO> abstractHistoryManager) {
        this.abstractHistoryManager = abstractHistoryManager;
    }

    public GroupedHistoryResponse<DTO> getGroupedHistory(Long userId) {
        return abstractHistoryManager.getGroupedHistory(userId);
    }
}