package com.example.demo.controller.linked_collection;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.dto.catalog.CartSummaryDTO;
import com.example.demo.service.cursor.CartCursorService;
import com.example.demo.service.linked_collection.CartService;
import com.example.demo.web.resolver.CurrentUserId;
import com.example.demo.web.util.ApiResponseFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/cart")
public class CartController extends AbstractLinkedCollectionController<CartItemViewDTO> {

    private final CartService cartService;

    public CartController(CartService cartService,
                          CartCursorService cartCursorService,
                          ApiResponseFactory apiResponseFactory) {
        super(cartService, cartCursorService, apiResponseFactory, "account/cart");
        this.cartService = cartService;
    }

    @Override
    public String getCollection(@ModelAttribute CursorPageRequest cursorPageRequest,
                                @CurrentUserId Long userId,
                                Model model) {
        populateCollectionModel(userId, cursorPageRequest, model);
        model.addAttribute("summary", cartService.getPurchasableSummary(userId));
        return viewName;
    }

    @GetMapping("/summary")
    @ResponseBody
    public CartSummaryDTO summary(@CurrentUserId Long userId) {
        return cartService.getPurchasableSummary(userId);
    }
}