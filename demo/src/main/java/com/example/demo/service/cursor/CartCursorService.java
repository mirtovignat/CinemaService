package com.example.demo.service.cursor;

import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.mapper.entity.CartItemMapper;
import com.example.demo.model.entity.CartItem;
import com.example.demo.repository.entity.CartItemRepository;
import com.example.demo.service.cursor.util.CursorCodec;
import com.example.demo.service.cursor.util.KeyUtils;
import com.example.demo.service.cursor.util.UserStatusMapperHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CartCursorService extends AbstractUserCursorService<CartItem, CartItemViewDTO> {

    public CartCursorService(CursorCodec cursorCodec,
                             CartItemRepository cartItemRepository,
                             UserStatusMapperHelper userStatusMapperHelper,
                             CartItemMapper cartItemMapper,
                             @Value("${app.catalog.page-size:20}") int defaultPageSize) {
        super(cursorCodec,
                defaultPageSize,
                cartItem -> KeyUtils.encodeTimestampAndIdentifier(
                        cartItem.getAddedAt(), cartItem.getAbstractCatalogItemId().getMovieId()),
                cartItems -> userStatusMapperHelper.mapWithStatuses(cartItems, cartItemMapper::toOwnerView),
                cartItemRepository::fetchAscending,
                cartItemRepository::fetchDescending);
    }
}