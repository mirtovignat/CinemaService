package com.example.demo.controller.linked_collection;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.cursor.context.UserCursorContext;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.cursor.CursorPageService;
import com.example.demo.service.linked_collection.AbstractLinkedCollectionService;
import com.example.demo.web.resolver.CurrentUserId;
import com.example.demo.web.util.ApiResponseFactory;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@AllArgsConstructor
public abstract class AbstractLinkedCollectionController<DTO> {

    protected final AbstractLinkedCollectionService<?, DTO> collectionService;
    protected final CursorPageService<UserCursorContext, DTO> cursorService;
    protected final ApiResponseFactory apiResponseFactory;
    protected final String viewName;

    @GetMapping
    public abstract String getCollection(@ModelAttribute CursorPageRequest cursorPageRequest,
                                         @CurrentUserId Long userId,
                                         Model model);

    protected void populateCollectionModel(Long userId,
                                           CursorPageRequest cursorPageRequest,
                                           Model model) {
        model.addAttribute("response",
                cursorService.page(
                        cursorPageRequest.cursor(),
                        cursorPageRequest.direction(),
                        UserCursorContext.builder()
                                .userId(userId)
                                .build()
                )
        );
        model.addAttribute("catalogCount", collectionService.getCount(userId));
    }

    @GetMapping("/more")
    @ResponseBody
    public CursorPageResponse<DTO> more(@ModelAttribute CursorPageRequest cursorPageRequest,
                                        @CurrentUserId Long userId) {
        return cursorService.page(
                cursorPageRequest.cursor(),
                cursorPageRequest.direction(),
                UserCursorContext.builder()
                        .userId(userId)
                        .build()
        );
    }

    @GetMapping("/count")
    @ResponseBody
    public Long count(@CurrentUserId(required = false) Long userId) {
        return collectionService.getNormalizedCount(userId);
    }

    @PostMapping("/add/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> add(@PathVariable Long id, @CurrentUserId Long userId) {
        collectionService.add(id, userId);
        return apiResponseFactory.ok(SuccessCode.ADDED, Map.of("count", collectionService.getCount(userId)));
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> remove(@PathVariable Long id, @CurrentUserId Long userId) {
        collectionService.remove(id, userId);
        return apiResponseFactory.ok(SuccessCode.REMOVED, Map.of("count", collectionService.getCount(userId)));
    }

    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<ApiResponse> clear(@CurrentUserId Long userId) {
        collectionService.removeAll(userId);
        return apiResponseFactory.ok(SuccessCode.CLEARED, Map.of("count", 0L));
    }
}