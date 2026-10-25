package com.example.demo.service.linked_collection;

import com.example.demo.dto.catalog.CartSummaryDTO;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.entity.CartItem;
import com.example.demo.repository.entity.CartItemRepository;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.repository.projection.CartSummaryProjection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CartService extends AbstractLinkedCollectionService<CartItem> {

    private final PurchasedMovieRepository purchasedMovieRepository;
    private final CartItemRepository cartItemRepository;

    public CartService(CartItemRepository cartItemRepository,
                       UserRepository userRepository,
                       MovieRepository movieRepository,
                       PurchasedMovieRepository purchasedMovieRepository) {
        super(cartItemRepository,
                userRepository,
                movieRepository,
                () -> CartItem.builder()
                        .quantity((short) 1)
                        .build(),
                ErrorCode.EMPTY_CART);
        this.purchasedMovieRepository = purchasedMovieRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Override
    public void add(Long movieId, Long userId) {
        if (purchasedMovieRepository.existsByMovieIdAndUserId(movieId, userId)) {
            return;
        }
        if (existsInCollection(movieId, userId)) {
            return;
        }
        CartItem preparedCartItem = prepare(movieId, userId);
        try {
            persist(preparedCartItem);
        } catch (DataIntegrityViolationException ignored) {
        }
    }

    public CartSummaryDTO getPurchasableSummary(Long userId) {
        return toSummaryDto(cartItemRepository.findPurchasableSummaryByUserId(userId));
    }

    private CartSummaryDTO toSummaryDto(CartSummaryProjection cartSummaryProjection) {
        if (cartSummaryProjection == null) {
            return CartSummaryDTO.empty();
        }
        long count = cartSummaryProjection.getCount() != null ? cartSummaryProjection.getCount() : 0L;
        BigDecimal total = cartSummaryProjection.getTotal() != null ? cartSummaryProjection.getTotal() : BigDecimal.ZERO;
        return CartSummaryDTO.builder()
                .count(count)
                .total(total)
                .build();
    }
}