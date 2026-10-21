package com.example.demo.service.catalog;

import com.example.demo.repository.entity.PurchasedMovieRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@AllArgsConstructor
@Service
public class PurchasedQueryService {

    private final PurchasedMovieRepository purchasedMovieRepository;

    @Transactional(readOnly = true)
    public boolean isMoviePurchasedByUser(Long movieId, Long userId) {
        if (movieId == null || userId == null) {
            return false;
        }
        return purchasedMovieRepository.existsByMovieIdAndUserId(
                movieId, userId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getPurchasedMovieIds(Long userId) {
        return new LinkedHashSet<>(purchasedMovieRepository
                .findMovieIdsByUserId(userId));
    }
}