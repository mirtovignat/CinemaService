package com.example.demo.service.linked_collection;

import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.dto.catalog.CartSummaryDTO;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.entity.CartItem;
import com.example.demo.repository.entity.CartItemRepository;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.repository.projection.CartSummaryProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CartService extends AbstractLinkedCollectionService<CartItem, CartItemViewDTO> {

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
    @Transactional
    public void add(Long movieId, Long userId) {
        if (purchasedMovieRepository.existsByMovieIdAndUserId(movieId, userId)) {
            return;
        }
        if (existsInCollection(movieId, userId)) {
            return;
        }
        save(movieId, userId);
    }

    @Transactional(readOnly = true)
    public CartSummaryDTO getPurchasableSummary(Long userId) {
        return toSummaryDto(cartItemRepository.findPurchasableSummaryByUserId(userId));
    }

    private CartSummaryDTO toSummaryDto(CartSummaryProjection projection) {
        if (projection == null) {
            return CartSummaryDTO.empty();
        }
        long count = projection.getCount() != null ? projection.getCount() : 0L;
        BigDecimal total = projection.getTotal() != null ? projection.getTotal() : BigDecimal.ZERO;
        return CartSummaryDTO.builder()
                .count(count)
                .total(total)
                .build();
    }
}