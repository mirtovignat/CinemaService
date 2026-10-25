package com.example.demo.service.linked_collection;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.model.entity.Movie;
import com.example.demo.model.entity.User;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.repository.entity.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Supplier;

@AllArgsConstructor
public abstract class AbstractLinkedCollectionService<Entity extends AbstractCatalogItem> {

    protected final AbstractCatalogItemRepository<Entity> abstractCatalogItemRepository;
    protected final UserRepository userRepository;
    protected final MovieRepository movieRepository;
    private final Supplier<Entity> entitySupplier;
    private final ErrorCode emptyCollectionErrorCode;

    public abstract void add(Long movieId, Long userId);

    public Long getCount(Long userId) {
        return abstractCatalogItemRepository.countByUserId(userId);
    }

    public boolean existsInCollection(Long movieId, Long userId) {
        return abstractCatalogItemRepository.existsByMovieIdAndUserId(movieId, userId);
    }

    public List<Entity> findAll(Long userId) {
        return abstractCatalogItemRepository.findAllByUserId(userId);
    }

    @Transactional
    public void remove(Long movieId, Long userId) {
        abstractCatalogItemRepository.deleteByMovieIdAndUserId(movieId, userId);
    }

    @Transactional
    public void deleteAll(Long userId) {
        abstractCatalogItemRepository.deleteAllByUserId(userId);
    }

    @Transactional
    public void removeAll(Long userId) {
        if (abstractCatalogItemRepository.countByUserId(userId) == 0) {
            throw BusinessException.of(emptyCollectionErrorCode);
        }
        abstractCatalogItemRepository.deleteAllByUserId(userId);
    }

    protected Entity prepare(Long movieId, Long userId) {
        User user = userRepository.findByIdOrThrow(userId);
        Movie movie = movieRepository.findByIdOrThrow(movieId);
        return AbstractCatalogItem.init(entitySupplier.get(), user, movie);
    }

    protected void persist(Entity entity) {
        abstractCatalogItemRepository.saveAndFlush(entity);
    }
}