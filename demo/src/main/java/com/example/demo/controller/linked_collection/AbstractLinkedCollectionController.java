package com.example.demo.controller.linked_collection;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.cursor.context.UserCursorContext;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CollectionCountResponse;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.cursor.CursorPageService;
import com.example.demo.service.cursor.SuggestableCursorService;
import com.example.demo.service.cursor.SuggestionService;
import com.example.demo.service.linked_collection.AbstractLinkedCollectionService;
import com.example.demo.web.resolver.CurrentUserId;
import com.example.demo.web.util.ApiResponseFactory;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
public abstract class AbstractLinkedCollectionController<DTO, Response> {

    protected final AbstractLinkedCollectionService<?> collectionService;
    protected final CursorPageService<UserCursorContext, DTO> cursorPageService;
    protected final SuggestionService suggestionService;
    protected final ApiResponseFactory apiResponseFactory;
    protected final String viewName;
    protected final String pageAttributeName;

    @GetMapping
    public String getCollection(@ModelAttribute
                                CursorPageRequest cursorPageRequest,
                                @CurrentUserId
                                Long userId,
                                Model model) {
        model.addAttribute(pageAttributeName, buildPageResponse(cursorPageRequest, userId));
        return viewName;
    }

    protected Response buildPageResponse(CursorPageRequest cursorPageRequest, Long userId) {
        CursorPageResponse<DTO> cursorPageResponse = cursorPageService.page(
                cursorPageRequest.cursor(),
                cursorPageRequest.direction(),
                cursorPageRequest.query(),
                UserCursorContext.builder()
                        .userId(userId)
                        .build()
        );
        return buildResponse(cursorPageResponse, userId, cursorPageRequest.query());
    }

    protected abstract Response buildResponse(CursorPageResponse<DTO> cursorPageResponse,
                                              Long userId,
                                              String query);

    @GetMapping("/more")
    @ResponseBody
    public CursorPageResponse<DTO> more(@ModelAttribute
                                        CursorPageRequest cursorPageRequest,
                                        @CurrentUserId
                                        Long userId) {
        return cursorPageService.page(
                cursorPageRequest.cursor(),
                cursorPageRequest.direction(),
                cursorPageRequest.query(),
                UserCursorContext.builder().userId(userId).build()
        );
    }

    @GetMapping("/count")
    @ResponseBody
    public Long count(@CurrentUserId(required = false)
                      Long userId) {
        if (userId == null) {
            return 0L;
        }
        return collectionService.getCount(userId);
    }

    @PostMapping("/add/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<CollectionCountResponse>> add(@PathVariable
                                                                    Long id,
                                                                    @CurrentUserId
                                                                    Long userId) {
        collectionService.add(id, userId);
        return apiResponseFactory.ok(SuccessCode.ADDED, CollectionCountResponse.builder()
                .count(collectionService.getCount(userId))
                .build());
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<CollectionCountResponse>> remove(@PathVariable
                                                                       Long id,
                                                                       @CurrentUserId
                                                                       Long userId) {
        collectionService.remove(id, userId);
        return apiResponseFactory.ok(SuccessCode.REMOVED, CollectionCountResponse.builder()
                .count(collectionService.getCount(userId))
                .build());
    }

    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<ApiResponse<CollectionCountResponse>> clear(@CurrentUserId
                                                                      Long userId) {
        collectionService.removeAll(userId);
        return apiResponseFactory.ok(SuccessCode.CLEARED, CollectionCountResponse.builder()
                .count(0L)
                .build());
    }

    @GetMapping("/suggestions")
    @ResponseBody
    public List<String> suggestions(@ModelAttribute
                                    CursorPageRequest cursorPageRequest,
                                    @CurrentUserId(required = false)
                                    Long userId) {
        if (!(cursorPageService instanceof SuggestableCursorService suggestableCursorService)) {
            return List.of();
        }
        return suggestionService.suggest(
                userId,
                cursorPageRequest.query(),
                (currentUserId, normalizedQuery) -> suggestableCursorService.findTitlesStartingWith(
                        currentUserId,
                        normalizedQuery,
                        PageRequest.of(0, 10)
                )
        );
    }
}