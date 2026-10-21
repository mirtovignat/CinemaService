package com.example.demo.controller.linked_collection;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.cursor.context.UserCursorContext;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.cursor.CursorPageService;
import com.example.demo.service.linked_collection.AbstractLinkedCollectionService;
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
    public String getCollection(@ModelAttribute CursorPageRequest cursorPageRequest,
                                SessionUser sessionUser,
                                Model model) {
        Long userId = sessionUser.id();
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
        return viewName;
    }

    @GetMapping("/more")
    @ResponseBody
    public CursorPageResponse<DTO> more(@ModelAttribute CursorPageRequest cursorPageRequest,
                                        SessionUser sessionUser) {
        return cursorService.page(
                cursorPageRequest.cursor(),
                cursorPageRequest.direction(),
                UserCursorContext.builder()
                        .userId(sessionUser.id())
                        .build()
        );
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