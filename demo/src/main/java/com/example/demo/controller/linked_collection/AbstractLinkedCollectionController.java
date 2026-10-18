package com.example.demo.controller.linked_collection;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.cursor.AbstractUserCursorService;
import com.example.demo.service.cursor.util.UserCursorContext;
import com.example.demo.service.linked_collection.AbstractLinkedCollectionService;
import com.example.demo.web.util.ApiResponseFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

public abstract class AbstractLinkedCollectionController<D> {

    protected final AbstractLinkedCollectionService<?, D> collectionService;
    protected final AbstractUserCursorService<?, D> cursorService;
    protected final ApiResponseFactory apiResponseFactory;
    protected final String viewName;

    protected AbstractLinkedCollectionController(AbstractLinkedCollectionService<?, D> collectionService,
                                                 AbstractUserCursorService<?, D> cursorService,
                                                 ApiResponseFactory apiResponseFactory,
                                                 String viewName) {
        this.collectionService = collectionService;
        this.cursorService = cursorService;
        this.apiResponseFactory = apiResponseFactory;
        this.viewName = viewName;
    }

    @GetMapping
    public String getCollection(@RequestParam(required = false) String cursor,
                                @RequestParam(required = false) String sort,
                                SessionUser sessionUser,
                                Model model) {
        Long userId = sessionUser.id();
        model.addAttribute("response", cursorService.page(cursor, sort, new UserCursorContext(userId)));
        model.addAttribute("catalogCount", collectionService.getCount(userId));
        return viewName;
    }

    @GetMapping("/more")
    @ResponseBody
    public CursorPageResponse<D> more(@RequestParam(required = false) String cursor,
                                      @RequestParam(required = false) String sort,
                                      SessionUser sessionUser) {
        return cursorService.page(cursor, sort, new UserCursorContext(sessionUser.id()));
    }

    @GetMapping("/count")
    @ResponseBody
    public Long count(SessionUser sessionUser) {
        Long userId = sessionUser != null ? sessionUser.id() : null;
        return collectionService.getNormalizedCount(userId);
    }

    @PostMapping("/add/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> add(@PathVariable Long id, SessionUser sessionUser) {
        Long userId = sessionUser.id();
        collectionService.add(id, userId);
        return apiResponseFactory.ok(SuccessCode.ADDED, Map.of("count", collectionService.getCount(userId)));
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> remove(@PathVariable Long id, SessionUser sessionUser) {
        Long userId = sessionUser.id();
        collectionService.remove(id, userId);
        return apiResponseFactory.ok(SuccessCode.REMOVED, Map.of("count", collectionService.getCount(userId)));
    }

    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<ApiResponse> clear(SessionUser sessionUser) {
        Long userId = sessionUser.id();
        collectionService.removeAll(userId);
        return apiResponseFactory.ok(SuccessCode.CLEARED, Map.of("count", 0L));
    }
}