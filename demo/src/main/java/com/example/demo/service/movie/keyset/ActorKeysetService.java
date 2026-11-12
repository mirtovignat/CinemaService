package com.example.demo.service.movie.keyset;

import com.example.demo.config.properties.FiltersProperties;
import com.example.demo.repository.entity.ActorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ActorKeysetService extends AbstractStringKeysetService {

    private final ActorRepository actorRepository;

    public ActorKeysetService(ActorRepository actorRepository,
                              StringKeysetPaginator paginator,
                              FiltersProperties filtersProperties) {
        super(paginator, filtersProperties.pageSize());
        this.actorRepository = actorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    protected List<String> fetchFirstPage(String normalizedQuery, int limit) {
        return actorRepository.fetchActorsKeysetFirstPage(normalizedQuery, limit);
    }

    @Override
    @Transactional(readOnly = true)
    protected List<String> fetchWithCursor(String normalizedQuery, String cursor, int limit) {
        return actorRepository.fetchActorsKeysetWithCursor(normalizedQuery, cursor, limit);
    }
}