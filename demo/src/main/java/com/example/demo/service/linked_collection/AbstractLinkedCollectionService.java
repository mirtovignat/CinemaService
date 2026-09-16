package com.example.demo.service.linked_collection;

import com.example.demo.dto.badges.BadgeCountsDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.model.entity.Movie;
import com.example.demo.model.entity.User;
import com.example.demo.repository.base.AbstractLinkedCollectionRepository;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
public abstract class AbstractLinkedCollectionService<
        Entity extends AbstractCatalogItem, DTO> {

    protected final AbstractLinkedCollectionRepository<Entity> linkedCollectionRepository;
    protected final UserRepository userRepository;
    protected final MovieRepository movieRepository;
    protected final PurchasedMovieRepository purchasedMovieRepository;

    @Transactional(readOnly = true)
    public Long getCount(Long userId) {
        return linkedCollectionRepository.countByUserId(userId);
    }

    public Long getNormalizedCount(Long userId){
        return userId == null ? 0 : getCount(userId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getMovieIds(Long userId) {
        return new LinkedHashSet<>(linkedCollectionRepository
                .findMovieIdsByUserId(userId));
    }

    @Transactional(readOnly = true)
    public List<DTO> getAllOfUser(Long userId) {
        return linkedCollectionRepository.findAllByUserId(userId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public void add(Long movieId, Long userId) {
        User user = userRepository.findByIdOrThrow(userId);
        Movie movie = movieRepository.findByIdOrThrow(movieId);
        if (shouldSkipIfPurchased() && purchasedMovieRepository
                .existsByMovieIdAndUserId(movie.getId(), userId)) {
            return;
        }
        if (existsInCollection(movie.getId(), userId)) {
            return;
        }
        Entity entity = createEntity(user, movie);
        entity.setUser(user);
        entity.setMovie(movie);
        linkedCollectionRepository.save(entity);
    }

    @Transactional
    public void remove(Long movieId, Long userId) {
        linkedCollectionRepository.deleteByMovieIdAndUserId(movieId, userId);
    }

    @Transactional
    public void removeAll(Long userId) {
        if (linkedCollectionRepository.countByUserId(userId) == 0) {
            throw BusinessException.of(getEmptyErrorCode());
        }
        linkedCollectionRepository.deleteAllByUserId(userId);
    }

    public void deleteAll(Long userId) {
        linkedCollectionRepository.deleteAllByUserId(userId);
    }

    protected abstract DTO mapToDto(Entity entity);

    protected abstract Entity createEntity(User user, Movie movie);

    protected abstract ErrorCode getEmptyErrorCode();

    protected boolean shouldSkipIfPurchased() {
        return false;
    }

    public boolean existsInCollection(Long movieId, Long userId) {
        return linkedCollectionRepository.existsByMovieIdAndUserId(
                movieId, userId);
    }

    public List<Entity> findAll(Long userId) {
        return linkedCollectionRepository.findAllByUserId(userId);
    }

    public abstract BadgeCountsDTO updateBadge(BadgeCountsDTO current, Long newCount);
}