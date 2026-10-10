package com.example.demo.service;

import com.example.demo.dto.badges.BadgeCountsDTO;
import com.example.demo.repository.entity.CartItemRepository;
import com.example.demo.repository.entity.FavouriteMovieRepository;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.RatedMovieRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class BadgeService {

    private final CartItemRepository cartItemRepository;
    private final FavouriteMovieRepository favouriteMovieRepository;
    private final PurchasedMovieRepository purchasedMovieRepository;
    private final RatedMovieRepository ratedMovieRepository;

    @Transactional(readOnly = true)
    public BadgeCountsDTO getBadgeCounts(Long id) {
        return BadgeCountsDTO.builder()
                .cartCount(cartItemRepository.countByUserId(id))
                .favouritesCount(favouriteMovieRepository.countByUserId(id))
                .purchasesCount(purchasedMovieRepository.countByUserId(id))
                .ratingsCount(ratedMovieRepository.countByUserId(id))
                .build();
    }
}