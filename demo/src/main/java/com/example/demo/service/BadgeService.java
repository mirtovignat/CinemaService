package com.example.demo.service;

import com.example.demo.dto.badges.BadgeCountsDTO;
import com.example.demo.repository.entity.CartItemRepository;
import com.example.demo.repository.entity.FavouriteMovieRepository;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.RatedMovieRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BadgeService {

    private final CartItemRepository cartItemRepository;
    private final FavouriteMovieRepository favouriteMovieRepository;
    private final PurchasedMovieRepository purchasedMovieRepository;
    private final RatedMovieRepository ratedMovieRepository;

    public BadgeCountsDTO getBadgeCounts(Long userId) {
        Long cartCount = cartItemRepository.countByUserId(userId);
        Long favouritesCount = favouriteMovieRepository.countByUserId(userId);
        Long purchasesCount = purchasedMovieRepository.countByUserId(userId);
        Long ratingsCount = ratedMovieRepository.countByUserId(userId);
        return BadgeCountsDTO.builder()
                .cartCount(cartCount)
                .favouritesCount(favouritesCount)
                .purchasesCount(purchasesCount)
                .ratingsCount(ratingsCount)
                .build();
    }
}