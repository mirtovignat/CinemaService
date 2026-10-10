package com.example.demo.service.catalog;

import com.example.demo.dto.response.CatalogResponse;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbstractCatalogService<CatalogItem extends AbstractCatalogItem, DTO> {

    protected final AbstractCatalogItemRepository<CatalogItem> catalogItemRepository;
    private final BiFunction<Long, CatalogItem, DTO> catalogItemMapper;

    protected AbstractCatalogService(AbstractCatalogItemRepository<CatalogItem> catalogItemRepository,
                                     BiFunction<Long, CatalogItem, DTO> catalogItemMapper) {
        this.catalogItemRepository = catalogItemRepository;
        this.catalogItemMapper = catalogItemMapper;
    }

    protected AbstractCatalogService(AbstractCatalogItemRepository<CatalogItem> catalogItemRepository,
                                     Function<CatalogItem, DTO> catalogItemMapper) {
        this(catalogItemRepository, (userId, item) -> catalogItemMapper.apply(item));
    }

    @Transactional(readOnly = true)
    public List<DTO> getCatalog(Long userId) {
        return catalogItemRepository.findAllByUserId(userId)
                .stream()
                .map(item -> catalogItemMapper.apply(userId, item))
                .toList();
    }

    @Transactional(readOnly = true)
    public CatalogResponse<DTO> buildCatalogResponse(UserForOwnerViewDTO userForOwnerView) {
        return CatalogResponse.<DTO>builder()
                .userForOwner(userForOwnerView)
                .catalog(getCatalog(userForOwnerView.id()))
                .build();
    }
}