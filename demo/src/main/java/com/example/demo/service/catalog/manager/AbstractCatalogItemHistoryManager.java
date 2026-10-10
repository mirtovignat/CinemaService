package com.example.demo.service.catalog.manager;

import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.util.HistoryGrouper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractCatalogItemHistoryManager<Entity extends AbstractCatalogItem, DTO>
        extends AbstractHistoryManager<Entity, DTO> {

    protected final AbstractCatalogItemRepository<Entity> abstractCatalogItemRepository;

    protected AbstractCatalogItemHistoryManager(UserRepository userRepository,
                                                HistoryGrouper historyGrouper,
                                                UserMapper userMapper,
                                                AbstractCatalogItemRepository<Entity> abstractCatalogItemRepository,
                                                Function<DTO, LocalDateTime> dateExtractor) {
        super(userRepository, historyGrouper, userMapper, dateExtractor);
        this.abstractCatalogItemRepository = abstractCatalogItemRepository;
    }

    @Override
    protected List<Entity> fetchEntities(Long userId) {
        return abstractCatalogItemRepository.findAllByUserId(userId);
    }
}