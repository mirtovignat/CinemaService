package com.example.demo.service.linked_collection;

import com.example.demo.dto.base.UserStatus;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.model.entity.Movie;
import com.example.demo.model.entity.User;
import com.example.demo.repository.base.AbstractCatalogItemRepository;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.repository.entity.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public abstract class AbstractLinkedCollectionService<Entity extends AbstractCatalogItem, DTO> {

    protected final AbstractCatalogItemRepository<Entity> abstractCatalogItemRepository;
    protected final UserRepository userRepository;
    protected final MovieRepository movieRepository;
    protected final UserStatusResolver userStatusResolver;

    private final Supplier<Entity> entitySupplier;
    private final BiFunction<Entity, UserStatus, DTO> dtoMapper;
    private final ErrorCode emptyErrorCode;

    protected AbstractLinkedCollectionService(AbstractCatalogItemRepository<Entity> abstractCatalogItemRepository,
                                              UserRepository userRepository,
                                              MovieRepository movieRepository,
                                              UserStatusResolver userStatusResolver,
                                              Supplier<Entity> entitySupplier,
                                              BiFunction<Entity, UserStatus, DTO> dtoMapper,
                                              ErrorCode emptyErrorCode) {
        this.abstractCatalogItemRepository = abstractCatalogItemRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.userStatusResolver = userStatusResolver;
        this.entitySupplier = entitySupplier;
        this.dtoMapper = dtoMapper;
        this.emptyErrorCode = emptyErrorCode;
    }

    @Transactional(readOnly = true)
    public Long getCount(Long userId) {
        return abstractCatalogItemRepository.countByUserId(userId);
    }

    public Long getNormalizedCount(Long userId) {
        return userId == null ? 0L : getCount(userId);
    }

    @Transactional(readOnly = true)
    public boolean existsInCollection(Long movieId, Long userId) {
        return abstractCatalogItemRepository.existsByMovieIdAndUserId(movieId, userId);
    }

    @Transactional(readOnly = true)
    public List<Entity> findAll(Long userId) {
        return abstractCatalogItemRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<DTO> getAllOfUser(Long userId) {
        List<Entity> entities = abstractCatalogItemRepository.findAllByUserId(userId);
        List<Long> movieIds = entities.stream()
                .map(entity -> entity.getMovie().getId())
                .toList();
        Map<Long, UserStatus> userStatuses = userStatusResolver.resolveAll(userId, movieIds);
        return entities.stream()
                .map(entity -> dtoMapper.apply(entity, userStatuses.get(entity.getMovie().getId())))
                .toList();
    }

    @Transactional
    public void add(Long movieId, Long userId) {
        if (existsInCollection(movieId, userId)) {
            return;
        }
        User user = userRepository.findByIdOrThrow(userId);
        Movie movie = movieRepository.findByIdOrThrow(movieId);
        abstractCatalogItemRepository.save(AbstractCatalogItem.init(entitySupplier.get(), user, movie));
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
            throw BusinessException.of(emptyErrorCode);
        }
        abstractCatalogItemRepository.deleteAllByUserId(userId);
    }
}