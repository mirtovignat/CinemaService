package com.example.demo.controller.linked_collection;

import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.catalog.cursor.CartCursorService;
import com.example.demo.service.cursor.context.UserCursorContext;
import com.example.demo.service.linked_collection.CartService;
import com.example.demo.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CartCursorService cartCursorService;
    private final MessageUtils messageUtils;

    @GetMapping
    public String getCart(@RequestParam(required = false) String cursor,
                          @RequestParam(required = false) String sort,
                          SessionUser sessionUser,
                          Model model) {
        Long userId = sessionUser.id();
        model.addAttribute("response", cartCursorService.page(cursor, sort, new UserCursorContext(userId)));
        model.addAttribute("catalogCount", cartService.getCount(userId));
        return "cart";
    }

    @GetMapping("/more")
    @ResponseBody
    public CursorPageResponse<CartItemViewDTO> more(@RequestParam(required = false) String cursor,
                                                    @RequestParam(required = false) String sort,
                                                    SessionUser sessionUser) {
        return cartCursorService.page(cursor, sort, new UserCursorContext(sessionUser.id()));
    }

    @GetMapping("/count")
    @ResponseBody
    public Long count(SessionUser sessionUser) {
        Long userId = sessionUser != null ? sessionUser.id() : null;
        return cartService.getNormalizedCount(userId);
    }

    @PostMapping("/add/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> add(@PathVariable Long id, SessionUser sessionUser) {
        Long userId = sessionUser.id();
        cartService.add(id, userId);
        return respond(SuccessCode.ADDED, cartService.getCount(userId));
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> remove(@PathVariable Long id, SessionUser sessionUser) {
        Long userId = sessionUser.id();
        cartService.remove(id, userId);
        return respond(SuccessCode.REMOVED, cartService.getCount(userId));
    }

    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<ApiResponse> clear(SessionUser sessionUser) {
        Long userId = sessionUser.id();
        cartService.removeAll(userId);
        return respond(SuccessCode.CLEARED, 0L);
    }

    private ResponseEntity<ApiResponse> respond(SuccessCode code, Long count) {
        return ResponseEntity.ok(ApiResponse.builder()
                .code(code.name())
                .message(messageUtils.getSuccess(code))
                .data(Map.of("count", count))
                .build());
    }
}