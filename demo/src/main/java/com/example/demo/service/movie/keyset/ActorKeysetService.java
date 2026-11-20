package com.example.demo.service.movie.keyset;

import com.example.demo.configuration.properties.FiltersProperties;
import com.example.demo.repository.entity.ActorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ActorKeysetService extends AbstractStringKeysetService {

    private final ActorRepository actorRepository;

    public ActorKeysetService(ActorRepository actorRepository,
                              StringKeysetPaginator stringKeysetPaginator,
                              FiltersProperties filtersProperties) {
        super(stringKeysetPaginator, filtersProperties.pageSize());
        this.actorRepository = actorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    protected List<String> fetchFirstPage(String searchPattern, int limit) {
        return actorRepository.fetchActorsKeysetFirstPage(searchPattern, limit);
    }

    @Override
    @Transactional(readOnly = true)
    protected List<String> fetchWithCursor(String searchPattern, String cursor, int limit) {
        return actorRepository.fetchActorsKeysetWithCursor(searchPattern, cursor, limit);
    }
}