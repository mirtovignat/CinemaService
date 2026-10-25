package com.example.demo.service.catalog;

import com.example.demo.repository.entity.PurchasedMovieRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@AllArgsConstructor
@Service
public class PurchasedQueryService {

    private final PurchasedMovieRepository purchasedMovieRepository;

    public boolean isMoviePurchasedByUser(Long movieId, Long userId) {
        if (movieId == null || userId == null) {
            return false;
        }
        return purchasedMovieRepository.existsByMovieIdAndUserId(movieId, userId);
    }

    public Set<Long> getPurchasedMovieIds(Long userId) {
        return new LinkedHashSet<>(purchasedMovieRepository.findMovieIdsByUserId(userId));
    }
}