package com.example.demo.controller.linked_collection;

import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.dto.catalog.CartSummaryDTO;
import com.example.demo.dto.response.CartPageResponse;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.service.cursor.CartCursorService;
import com.example.demo.service.cursor.SuggestionService;
import com.example.demo.service.linked_collection.CartService;
import com.example.demo.web.resolver.CurrentUserId;
import com.example.demo.web.util.ApiResponseFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/cart")
public class CartController extends AbstractLinkedCollectionController<CartItemViewDTO, CartPageResponse<CartItemViewDTO>> {

    private final CartService cartService;

    public CartController(CartService cartService,
                          CartCursorService cartCursorService,
                          SuggestionService suggestionService,
                          ApiResponseFactory apiResponseFactory) {
        super(cartService,
                cartCursorService,
                suggestionService,
                apiResponseFactory,
                "account/cart",
                "cartPageResponse");
        this.cartService = cartService;
    }

    @Override
    protected CartPageResponse<CartItemViewDTO> buildResponse(CursorPageResponse<CartItemViewDTO>
                                                                      cursorPageResponse,
                                                              Long userId,
                                                              String query) {
        return CartPageResponse.<CartItemViewDTO>builder()
                .cursorPageResponse(cursorPageResponse)
                .cartSummaryDTO(cartService.getPurchasableSummary(userId))
                .catalogCount(collectionService.getCount(userId))
                .query(query)
                .build();
    }

    @GetMapping("/summary")
    @ResponseBody
    public CartSummaryDTO summary(@CurrentUserId Long userId) {
        return cartService.getPurchasableSummary(userId);
    }
}