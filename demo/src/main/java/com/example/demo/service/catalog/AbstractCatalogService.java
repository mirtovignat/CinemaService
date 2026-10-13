package com.example.demo.service.catalog;

import com.example.demo.dto.response.CatalogResponse;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

@AllArgsConstructor
public abstract class AbstractCatalogService<CatalogItem extends AbstractCatalogItem, DTO> {

    protected final AbstractCatalogItemRepository<CatalogItem> catalogItemRepository;
    private final Function<CatalogItem, DTO> catalogItemMapper;

    @Transactional(readOnly = true)
    public List<DTO> getCatalog(Long userId) {
        return catalogItemRepository.findAllByUserId(userId)
                .stream()
                .map(catalogItemMapper)
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