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

    private final CartService cartService;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final PurchasedMovieRepository purchasedMovieRepository;
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
            throw BusinessException.of(ErrorCode.EMPTY_CART);
        }
        List<Long> movieIds = cartItems.stream()
                .map(cartItem -> cartItem.getMovie().getId())
                .toList();
        return purchaseMovies(movieIds, userId, false);
    }

    private BigDecimal purchaseMovies(List<Long> requestedMovieIds, Long userId,
                                      boolean isSinglePurchase) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND, "id=" + userId));

        Set<Long> alreadyPurchasedMovieIds = purchasedQueryService.getPurchasedMovieIds(userId);
        List<Long> movieIdsToBuy = requestedMovieIds.stream()
                .filter(movieId -> !alreadyPurchasedMovieIds.contains(movieId))
                .distinct()
                .toList();

        if (movieIdsToBuy.isEmpty()) {
            clearCartFor(requestedMovieIds, userId, isSinglePurchase);
            return user.getBalance();
        }

        List<Movie> moviesToBuy = movieRepository.findAllById(movieIdsToBuy);
        if (moviesToBuy.size() != movieIdsToBuy.size()) {
            throw BusinessException.of(ErrorCode.ENTITY_NOT_FOUND);
        }

        List<PurchasedMovie> purchasesToSave = buildPurchases(user, moviesToBuy);
        BigDecimal totalPrice = calculateTotalPrice(purchasesToSave);

        if (user.getBalance().compareTo(totalPrice) < 0) {
            throw BusinessException.of(ErrorCode.INSUFFICIENT_FUNDS, totalPrice, user.getBalance());
        }
        user.spendMoney(totalPrice);
        userRepository.save(user);
        purchasedMovieRepository.saveAll(purchasesToSave);

        clearCartFor(requestedMovieIds, userId, isSinglePurchase);
        return user.getBalance();
    }

    private List<PurchasedMovie> buildPurchases(User user, List<Movie> moviesToBuy) {
        return moviesToBuy.stream()
                .map(movie -> {
                    PurchasedMovie purchasedMovie = PurchasedMovie.builder().build();
                    purchasedMovie.setPriceSnapshot(movie.getPrice());
                    return AbstractCatalogItem.init(purchasedMovie, user, movie);
                })
                .toList();
    }

    private BigDecimal calculateTotalPrice(List<PurchasedMovie> purchasesToSave) {
        return purchasesToSave.stream()
                .map(PurchasedMovie::getPriceSnapshot)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void clearCartFor(List<Long> requestedMovieIds, Long userId, boolean isSinglePurchase) {
        if (isSinglePurchase && !requestedMovieIds.isEmpty()) {
            cartService.remove(requestedMovieIds.get(0), userId);
        } else {
            cartService.deleteAll(userId);
        }
    }
}