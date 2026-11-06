package com.example.demo.service.cursor;

import com.example.demo.config.properties.CatalogProperties;
import com.example.demo.cursor.UserStatusMapperHelper;
import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.IdCodec;
import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.mapper.entity.CartItemMapper;
import com.example.demo.model.entity.CartItem;
import com.example.demo.repository.entity.CartItemRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartCursorService extends AbstractUserCursorService<CartItem, CartItemViewDTO>
        implements SuggestableCursorService {

    private final CartItemRepository cartItemRepository;

    public CartCursorService(CursorCodec cursorCodec,
                             IdCodec idCodec,
                             CartItemRepository cartItemRepository,
                             UserStatusMapperHelper userStatusMapperHelper,
                             CartItemMapper cartItemMapper,
                             CatalogProperties catalogProperties) {
        super(cursorCodec,
                idCodec,
                catalogProperties.cursorPageResponseSize(),
                cartItem -> idCodec.encode(cartItem.getId()),
                cartItems -> userStatusMapperHelper.mapWithStatuses(cartItems, cartItemMapper::toOwnerView),
                cartItemRepository::fetchAscending,
                cartItemRepository::fetchDescending);
        this.cartItemRepository = cartItemRepository;
    }

    @Override
    public List<String> findTitlesStartingWith(Long userId, String searchQuery, Pageable pageable) {
        return cartItemRepository.findTitlesStartingWith(userId, searchQuery, pageable);
    }
}