package com.example.demo.service.movie;

import com.example.demo.repository.entity.CartItemRepository;
import com.example.demo.repository.entity.FavouriteMovieRepository;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.RatedMovieRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@AllArgsConstructor
@Service
public class MovieRecommendationService {

    private final CartItemRepository cartItemRepository;
    private final FavouriteMovieRepository favouriteMovieRepository;
    private final PurchasedMovieRepository purchasedMovieRepository;
    private final RatedMovieRepository ratedMovieRepository;

    @Transactional(readOnly = true)
    public Set<Long> collectExcludedMovieIds(Long userId) {
        Set<Long> excludedMovieIds = new LinkedHashSet<>();
        if (userId == null) {
            return excludedMovieIds;
        }
        excludedMovieIds.addAll(cartItemRepository.findMovieIdsByUserId(userId));
        excludedMovieIds.addAll(favouriteMovieRepository.findMovieIdsByUserId(userId));
        excludedMovieIds.addAll(purchasedMovieRepository.findMovieIdsByUserId(userId));
        excludedMovieIds.addAll(ratedMovieRepository.findMovieIdsByUserId(userId));
        return excludedMovieIds;
    }
}