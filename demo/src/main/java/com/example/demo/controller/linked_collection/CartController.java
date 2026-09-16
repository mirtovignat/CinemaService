package com.example.demo.controller.linked_collection;

import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.service.linked_collection.CartService;
import com.example.demo.util.MessageUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cart")
public class CartController extends AbstractLinkedCollectionController<CartItemViewDTO> {

    public CartController(CartService cartService,
                          MessageUtils messageUtils) {
        super("cart", cartService, messageUtils);
    }
}