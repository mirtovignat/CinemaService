package com.example.demo.service;

import com.example.demo.dto.badges.BadgeCountsDTO;
import com.example.demo.repository.entity.CartItemRepository;
import com.example.demo.repository.entity.FavouriteMovieRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BadgeService {

    private final CartItemRepository cartItemRepository;
    private final FavouriteMovieRepository favouriteMovieRepository;

    public BadgeCountsDTO getBadgeCounts(Long userId) {
        Long cartCount = cartItemRepository.countByUserId(userId);
        Long favouritesCount = favouriteMovieRepository.countByUserId(userId);
        return BadgeCountsDTO.builder()
                .cartCount(cartCount)
                .favouritesCount(favouritesCount)
                .build();
    }
}