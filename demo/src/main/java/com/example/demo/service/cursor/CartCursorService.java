package com.example.demo.service.cursor;

import com.example.demo.cursor.UserStatusMapperHelper;
import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.KeyCodec;
import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.mapper.entity.CartItemMapper;
import com.example.demo.model.entity.CartItem;
import com.example.demo.repository.entity.CartItemRepository;
import com.example.demo.util.normalizer.TitleNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartCursorService extends AbstractUserCursorService<CartItem, CartItemViewDTO> {

    private final CartItemRepository cartItemRepository;

    public CartCursorService(CursorCodec cursorCodec,
                             KeyCodec keyCodec,
                             TitleNormalizer titleNormalizer,
                             CartItemRepository cartItemRepository,
                             UserStatusMapperHelper userStatusMapperHelper,
                             CartItemMapper cartItemMapper,
                             @Value("${app.catalog.cursorPageResponse-size:20}") int defaultPageSize) {
        super(cursorCodec,
                keyCodec,
                titleNormalizer,
                defaultPageSize,
                cartItem -> keyCodec.encode(
                        cartItem.getAddedAt(),
                        cartItem.getAbstractCatalogItemId().getMovieId()
                ),
                cartItems -> userStatusMapperHelper.mapWithStatuses(
                        cartItems,
                        cartItemMapper::toOwnerView
                ),
                cartItemRepository::fetchAscending,
                cartItemRepository::fetchDescending);
        this.cartItemRepository = cartItemRepository;
    }

    public List<String> findTitlesStartingWith(Long userId, String query, Pageable pageable) {
        return cartItemRepository.findTitlesStartingWith(userId, query, pageable);
    }
}