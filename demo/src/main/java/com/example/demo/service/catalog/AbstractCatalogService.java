package com.example.demo.service.catalog;

import com.example.demo.dto.response.CatalogResponse;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.repository.base.AbstractCatalogRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

public abstract class AbstractCatalogService<CatalogItem extends AbstractCatalogItem, DTO> {

    protected final AbstractCatalogRepository<CatalogItem> catalogRepository;

    protected AbstractCatalogService(AbstractCatalogRepository<CatalogItem> catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    protected abstract Function<CatalogItem, DTO> getMapper();

    @Transactional(readOnly = true)
    public List<DTO> getCatalog(Long id) {
        return catalogRepository.findAllByUserId(id)
                .stream()
                .map(getMapper())
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean existsByMovieIdAndUserId(Long movieId, Long userId) {
        return catalogRepository.existsByMovieIdAndUserId(movieId, userId);
    }

    @Transactional(readOnly = true)
    public CatalogResponse<DTO> buildCatalogResponse(UserForOwnerViewDTO userForOwnerView) {
        return CatalogResponse.<DTO>builder()
                .userForOwner(userForOwnerView)
                .catalog(getCatalog(userForOwnerView.id()))
                .build();
    }
}