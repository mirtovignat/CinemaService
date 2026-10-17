package com.example.demo.service.catalog.cursor;

import com.example.demo.dto.base.UserStatus;
import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.mapper.entity.CartItemMapper;
import com.example.demo.model.entity.CartItem;
import com.example.demo.repository.entity.CartItemRepository;
import com.example.demo.service.cursor.AbstractCursorService;
import com.example.demo.service.cursor.CursorCodec;
import com.example.demo.service.cursor.KeyUtils;
import com.example.demo.service.cursor.context.UserCursorContext;
import com.example.demo.service.linked_collection.UserStatusResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CartCursorService
        extends AbstractCursorService<CartItem, CartItemViewDTO, UserCursorContext> {

    private final CartItemRepository cartItemRepository;

    public CartCursorService(CursorCodec cursorCodec,
                             CartItemRepository cartItemRepository,
                             UserStatusResolver userStatusResolver,
                             CartItemMapper cartItemMapper,
                             @Value("${app.catalog.page-size:20}") int defaultPageSize) {
        super(cursorCodec,
                defaultPageSize,
                cartItem -> KeyUtils.encodeTimestampAndIdentifier(
                        cartItem.getAddedAt(), cartItem.getAbstractCatalogItemId().getMovieId()),
                cartItems -> mapWithStatuses(cartItems, userStatusResolver, cartItemMapper));
        this.cartItemRepository = cartItemRepository;
    }

    @Override
    protected Slice<CartItem> fetchSlice(String cursorValue,
                                         String sortDirection,
                                         PageRequest pageRequest,
                                         UserCursorContext context) {
        Long userId = context.userId();
        boolean hasCursor = cursorValue != null;
        LocalDateTime cursorAddedAt = hasCursor ? KeyUtils.decodeTimestamp(cursorValue) : null;
        Long cursorMovieId = hasCursor ? KeyUtils.decodeIdentifier(cursorValue) : null;
        return "asc".equals(sortDirection)
                ? cartItemRepository.fetchAscending(userId, hasCursor, cursorAddedAt, cursorMovieId, pageRequest)
                : cartItemRepository.fetchDescending(userId, hasCursor, cursorAddedAt, cursorMovieId, pageRequest);
    }

    private static List<CartItemViewDTO> mapWithStatuses(List<CartItem> cartItems,
                                                         UserStatusResolver userStatusResolver,
                                                         CartItemMapper cartItemMapper) {
        if (cartItems.isEmpty()) {
            return List.of();
        }
        Long userId = cartItems.get(0).getAbstractCatalogItemId().getUserId();
        List<Long> movieIds = cartItems.stream()
                .map(cartItem -> cartItem.getAbstractCatalogItemId().getMovieId())
                .toList();
        Map<Long, UserStatus> userStatuses = userStatusResolver.resolveAll(userId, movieIds);
        return cartItems.stream()
                .map(cartItem -> cartItemMapper.toOwnerView(
                        cartItem,
                        userStatuses.getOrDefault(cartItem.getAbstractCatalogItemId().getMovieId(),
                                UserStatus.builder().build())))
                .toList();
    }
}