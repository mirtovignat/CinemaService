package com.example.demo.service.catalog;

import com.example.demo.dto.base.PageCursor;
import com.example.demo.dto.base.UserStatus;
import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.mapper.entity.CartItemMapper;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.entity.CartItem;
import com.example.demo.repository.entity.CartItemRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.service.cursor.AbstractCursorService;
import com.example.demo.service.linked_collection.UserStatusResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
public class CartCatalogService extends AbstractCursorService<CartItem, CartItemViewDTO> {

    private final CartItemRepository cartItemRepository;

    public CartCatalogService(CartItemRepository cartItemRepository,
                              UserRepository userRepository,
                              UserMapper userMapper,
                              UserStatusResolver userStatusResolver,
                              CartItemMapper cartItemMapper,
                              @Value("${app.catalog.page-size:20}") int defaultPageSize) {
        super(userRepository,
                userMapper,
                entities -> mapWithStatuses(entities, userStatusResolver, cartItemMapper),
                CartCatalogService::extractCursor,
                defaultPageSize);
        this.cartItemRepository = cartItemRepository;
    }

    private static List<CartItemViewDTO> mapWithStatuses(List<CartItem> items,
                                                         UserStatusResolver resolver,
                                                         CartItemMapper mapper) {
        if (items.isEmpty()) return List.of();
        Long userId = items.get(0).getId().getUserId();
        List<Long> movieIds = items.stream()
                .map(item -> item.getId().getMovieId())
                .toList();
        Map<Long, UserStatus> statuses = resolver.resolveAll(userId, movieIds);
        return items.stream()
                .map(item -> mapper.toOwnerView(
                        item,
                        statuses.getOrDefault(item.getId().getMovieId(),
                                UserStatus.builder().build())))
                .toList();
    }

    private static PageCursor extractCursor(CartItem item) {
        return PageCursor.builder()
                .addedAt(item.getAddedAt().toInstant(ZoneOffset.UTC))
                .entityId(item.getId().getMovieId())
                .build();
    }

    @Override
    protected Slice<CartItem> fetchSlice(Long userId,
                                         PageCursor pageCursor,
                                         boolean isAscending,
                                         Pageable pageable) {
        if (pageCursor == null) {
            return isAscending
                    ? cartItemRepository.findPageAscFirst(userId, pageable)
                    : cartItemRepository.findPageDescFirst(userId, pageable);
        }
        return isAscending
                ? cartItemRepository.findPageAscAfter(userId, pageCursor.addedAtUtc(), pageCursor.entityId(), pageable)
                : cartItemRepository.findPageDescAfter(userId, pageCursor.addedAtUtc(), pageCursor.entityId(), pageable);
    }
}