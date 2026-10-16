package com.example.demo.controller.linked_collection;

import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.catalog.FavouritesCatalogService;
import com.example.demo.service.linked_collection.FavouritesService;
import com.example.demo.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/favourites")
@RequiredArgsConstructor
public class FavouritesController {

    private final FavouritesService favouritesService;
    private final FavouritesCatalogService favouritesCatalogService;
    private final MessageUtils messageUtils;

    @GetMapping
    public String getFavourites(@RequestParam(required = false) String cursor,
                                @RequestParam(required = false) String sort,
                                @RequestParam(required = false) Integer size,
                                SessionUser sessionUser,
                                Model model) {
        Long userId = sessionUser.id();
        model.addAttribute("response", favouritesCatalogService.getPage(userId, cursor, sort, size));
        model.addAttribute("catalogCount", favouritesService.getCount(userId));
        return "favourites";
    }

    @GetMapping("/more")
    @ResponseBody
    public CursorPageResponse<FavouriteMovieViewDTO> more(@RequestParam(required = false) String cursor,
                                                          @RequestParam(required = false) String sort,
                                                          @RequestParam(required = false) Integer size,
                                                          SessionUser sessionUser) {
        return favouritesCatalogService.getPage(sessionUser.id(), cursor, sort, size);
    }

    @GetMapping("/count")
    @ResponseBody
    public Long count(SessionUser sessionUser) {
        Long userId = sessionUser != null ? sessionUser.id() : null;
        return favouritesService.getNormalizedCount(userId);
    }

    @PostMapping("/add/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> add(@PathVariable Long id, SessionUser sessionUser) {
        Long userId = sessionUser.id();
        favouritesService.add(id, userId);
        return respond(SuccessCode.ADDED, favouritesService.getCount(userId));
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> remove(@PathVariable Long id, SessionUser sessionUser) {
        Long userId = sessionUser.id();
        favouritesService.remove(id, userId);
        return respond(SuccessCode.REMOVED, favouritesService.getCount(userId));
    }

    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<ApiResponse> clear(SessionUser sessionUser) {
        Long userId = sessionUser.id();
        favouritesService.removeAll(userId);
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