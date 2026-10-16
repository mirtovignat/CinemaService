package com.example.demo.controller.linked_collection;

import com.example.demo.controller.catalog.AbstractCursorController;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.SuccessCode;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.service.cursor.AbstractCursorService;
import com.example.demo.service.linked_collection.AbstractLinkedCollectionService;
import com.example.demo.util.MessageUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

public abstract class AbstractCursorCollectionController<Entity extends AbstractCatalogItem, DTO>
        extends AbstractCursorController<DTO> {

    protected final AbstractLinkedCollectionService<Entity, DTO> collectionService;
    protected final MessageUtils messageUtils;

    protected AbstractCursorCollectionController(String viewName,
                                                 AbstractCursorService<?, DTO> cursorService,
                                                 AbstractLinkedCollectionService<Entity, DTO> collectionService,
                                                 MessageUtils messageUtils) {
        super(viewName, cursorService, false);
        this.collectionService = collectionService;
        this.messageUtils = messageUtils;
    }

    @Override
    @GetMapping
    public String getList(Model model,
                          SessionUser sessionUser,
                          @RequestParam(value = "sort", required = false) String sortDirection,
                          @RequestParam(value = "cursor", required = false) String rawCursor,
                          @RequestParam(value = "size", required = false) Integer requestedPageSize) {
        String view = super.getList(model, sessionUser, sortDirection, rawCursor, requestedPageSize);
        model.addAttribute("catalogCount", collectionService.getCount(sessionUser.id()));
        return view;
    }

    @GetMapping("/count")
    @ResponseBody
    public Long getCount(SessionUser sessionUser) {
        return collectionService.getNormalizedCount(sessionUser.id());
    }

    @PostMapping("/add/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> add(@PathVariable Long id, SessionUser sessionUser) {
        Long userId = sessionUser.id();
        collectionService.add(id, userId);
        return respond(SuccessCode.ADDED, collectionService.getCount(userId));
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> remove(@PathVariable Long id, SessionUser sessionUser) {
        Long userId = sessionUser.id();
        collectionService.remove(id, userId);
        return respond(SuccessCode.REMOVED, collectionService.getCount(userId));
    }

    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<ApiResponse> clear(SessionUser sessionUser) {
        Long userId = sessionUser.id();
        collectionService.removeAll(userId);
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