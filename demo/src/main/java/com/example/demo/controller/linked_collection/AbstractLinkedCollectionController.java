package com.example.demo.controller.linked_collection;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.SuccessCode;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.service.linked_collection.AbstractLinkedCollectionService;
import com.example.demo.util.MessageUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@AllArgsConstructor
public abstract class AbstractLinkedCollectionController<Entity extends AbstractCatalogItem, DTO> {

    @Getter
    private final String viewName;
    protected final AbstractLinkedCollectionService<Entity, DTO> abstractLinkedCollectionService;
    protected final MessageUtils messageUtils;

    @GetMapping
    public String getCatalog(Model model, SessionUser sessionUser) {
        Long userId = sessionUser.id();
        model.addAttribute("catalog", abstractLinkedCollectionService.getAllOfUser(userId));
        model.addAttribute("catalogCount", abstractLinkedCollectionService.getCount(userId));
        return viewName;
    }

    @GetMapping("/count")
    @ResponseBody
    public Long getCount(SessionUser sessionUser) {
        Long userId = sessionUser != null ? sessionUser.id() : null;
        return abstractLinkedCollectionService.getNormalizedCount(userId);
    }

    @PostMapping("/add/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> add(@PathVariable Long id, SessionUser sessionUser) {
        Long userId = sessionUser.id();
        abstractLinkedCollectionService.add(id, userId);
        return respond(SuccessCode.ADDED, abstractLinkedCollectionService.getCount(userId));
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> remove(@PathVariable Long id, SessionUser sessionUser) {
        Long userId = sessionUser.id();
        abstractLinkedCollectionService.remove(id, userId);
        return respond(SuccessCode.REMOVED, abstractLinkedCollectionService.getCount(userId));
    }

    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<ApiResponse> clear(SessionUser sessionUser) {
        Long userId = sessionUser.id();
        abstractLinkedCollectionService.removeAll(userId);
        return respond(SuccessCode.CLEARED, 0L);
    }

    private ResponseEntity<ApiResponse> respond(SuccessCode successCode, Long count) {
        return ResponseEntity.ok(ApiResponse.builder()
                .code(successCode.name())
                .message(messageUtils.getSuccess(successCode))
                .data(Map.of("count", count))
                .build());
    }
}