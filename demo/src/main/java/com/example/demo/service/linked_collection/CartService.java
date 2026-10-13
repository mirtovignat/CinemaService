package com.example.demo.service.linked_collection;

import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.entity.CartItemMapper;
import com.example.demo.model.entity.CartItem;
import com.example.demo.repository.entity.CartItemRepository;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService extends AbstractLinkedCollectionService<CartItem, CartItemViewDTO> {

    private final PurchasedMovieRepository purchasedMovieRepository;

    public CartService(CartItemRepository cartItemRepository,
                       UserRepository userRepository,
                       MovieRepository movieRepository,
                       UserStatusResolver userStatusResolver,
                       PurchasedMovieRepository purchasedMovieRepository,
                       CartItemMapper cartItemMapper) {
        super(cartItemRepository, userRepository, movieRepository, userStatusResolver,
                () -> CartItem.builder().quantity((short) 1).build(),
                cartItemMapper::toOwnerView,
                ErrorCode.EMPTY_CART);
        this.purchasedMovieRepository = purchasedMovieRepository;
    }

    @Override
    @Transactional
    public void add(Long movieId, Long userId) {
        if (purchasedMovieRepository.existsByMovieIdAndUserId(movieId, userId)) {
            return;
        }
        super.add(movieId, userId);
    }
}