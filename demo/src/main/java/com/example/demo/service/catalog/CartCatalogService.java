// demo/src/main/java/com/example/demo/service/catalog/CartCatalogService.java
package com.example.demo.service.catalog;

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
                cartItem -> cartItem.getId().getMovieId(),
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

    @Override
    protected Slice<CartItem> fetchSlice(Long userId,
                                         Long cursorValue,
                                         boolean isAscending,
                                         Pageable pageable) {
        return isAscending
                ? cartItemRepository.findPageAsc(userId, cursorValue, pageable)
                : cartItemRepository.findPageDesc(userId, cursorValue, pageable);
    }
}