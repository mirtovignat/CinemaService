package com.example.demo.controller.linked_collection;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.dto.catalog.CartSummaryDTO;
import com.example.demo.service.cursor.CartCursorService;
import com.example.demo.service.cursor.SuggestionService;
import com.example.demo.service.linked_collection.CartService;
import com.example.demo.web.resolver.CurrentUserId;
import com.example.demo.web.util.ApiResponseFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController extends AbstractLinkedCollectionController<CartItemViewDTO> {

    private final CartService cartService;
    private final CartCursorService cartCursorService;
    private final SuggestionService suggestionService;

    public CartController(CartService cartService,
                          CartCursorService cartCursorService,
                          SuggestionService suggestionService,
                          ApiResponseFactory apiResponseFactory) {
        super(cartService, cartCursorService, apiResponseFactory, "account/cart");
        this.cartService = cartService;
        this.cartCursorService = cartCursorService;
        this.suggestionService = suggestionService;
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

    @Override
    @GetMapping("/suggestions")
    @ResponseBody
    public List<String> suggestions(@RequestParam(value = "query", required = false, defaultValue = "") String query,
                                    @CurrentUserId(required = false) Long userId) {
        return suggestionService.suggest(userId, query,
                (uId, normalizedQuery) -> cartCursorService.findTitlesStartingWith(uId, normalizedQuery, PageRequest.of(0, 10)));
    }
}