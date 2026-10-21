package com.example.demo.service.catalog;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.model.entity.CartItem;
import com.example.demo.model.entity.Movie;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.model.entity.User;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.service.linked_collection.CartService;
import com.example.demo.service.user.UserQueryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Service
public class PurchasedCommandService {

    private final PurchasedMovieRepository purchasedMovieRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final MovieRepository movieRepository;
    private final UserQueryService userQueryService;
    private final PurchasedQueryService purchasedQueryService;

    @Transactional
    public BigDecimal purchase(Long movieId, Long userId) {
        return purchaseMovies(List.of(movieId), userId, true);
    }

    @Transactional
    public BigDecimal purchaseBulk(Long userId) {
        List<CartItem> cartItems = cartService.findAll(userId);
        if (cartItems.isEmpty()) {
            return userQueryService.getBalance(userId);
        }
        List<Long> movieIds = cartItems.stream()
                .map(item -> item.getMovie().getId())
                .toList();
        return purchaseMovies(movieIds, userId, false);
    }

    private BigDecimal purchaseMovies(List<Long> requestedMovieIds, Long userId,
                                      boolean isSinglePurchase) {
        User user = userQueryService.getUser(userId);
        Set<Long> alreadyPurchasedIds = purchasedQueryService.getPurchasedMovieIds(userId);
        List<Long> idsToBuy = requestedMovieIds.stream()
                .filter(id -> !alreadyPurchasedIds.contains(id))
                .distinct()
                .toList();

        if (idsToBuy.isEmpty()) {
            clearCartFor(requestedMovieIds, userId, isSinglePurchase);
            return user.getBalance();
        }
        List<Movie> moviesToBuy = movieRepository.findAllById(idsToBuy);

        if (moviesToBuy.size() != idsToBuy.size()) {
            throw BusinessException.of(ErrorCode.ENTITY_NOT_FOUND);
        }

        BigDecimal totalPrice = moviesToBuy.stream()
                .map(Movie::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (user.getBalance().compareTo(totalPrice) < 0) {
            throw BusinessException.of(ErrorCode.INSUFFICIENT_FUNDS, totalPrice, user.getBalance());
        }
        user.spendMoney(totalPrice);
        userRepository.save(user);
        List<PurchasedMovie> purchases = moviesToBuy.stream()
                .map(movie -> {
                    PurchasedMovie purchased = PurchasedMovie.builder().build();
                    purchased.setPriceSnapshot(movie.getPrice());
                    return AbstractCatalogItem.init(purchased, user, movie);
                })
                .toList();
        purchasedMovieRepository.saveAll(purchases);
        clearCartFor(requestedMovieIds, userId, isSinglePurchase);
        return user.getBalance();
    }

    private void clearCartFor(List<Long> requestedMovieIds, Long userId, boolean isSinglePurchase) {
        if (isSinglePurchase && !requestedMovieIds.isEmpty()) {
            cartService.remove(requestedMovieIds.get(0), userId);
        } else {
            cartService.deleteAll(userId);
        }
    }
}