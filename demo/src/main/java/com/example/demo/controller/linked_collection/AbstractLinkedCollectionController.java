package com.example.demo.controller.linked_collection;

import com.example.demo.dto.badges.BadgeCountsDTO;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.linked_collection.AbstractLinkedCollectionService;
import com.example.demo.util.MessageUtils;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@AllArgsConstructor
public abstract class AbstractLinkedCollectionController<DTO> {

    @Getter
    private final String viewName;
    protected final AbstractLinkedCollectionService<?, DTO> linkedCollectionService;
    protected final MessageUtils messageUtils;

    @GetMapping
    public String getCatalog(Model model,
                             @SessionAttribute SessionUser sessionUser) {
        Long userId = sessionUser.id();
        model.addAttribute("catalog", linkedCollectionService.getAllOfUser(userId));
        model.addAttribute("catalogCount", linkedCollectionService.getCount(userId));
        return getViewName();
    }

    @GetMapping("/count")
    @ResponseBody
    public Long getCount(@SessionAttribute(required = false) SessionUser sessionUser) {
        Long userId = sessionUser != null ? sessionUser.id() : null;
        return linkedCollectionService.getNormalizedCount(userId);
    }

    @PostMapping("/add/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> add(@PathVariable Long id,
                                           @SessionAttribute SessionUser sessionUser,
                                           HttpSession httpSession) {
        Long userId = sessionUser.id();
        linkedCollectionService.add(id, userId);
        Long newCount = linkedCollectionService.getCount(userId);
        updateBadgeInSession(httpSession, newCount);
        String message = messageUtils.getSuccess(SuccessCode.ADDED);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(SuccessCode.ADDED.name())
                .message(message)
                .data(Map.of("count", newCount))
                .build());
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> remove(@PathVariable Long id,
                                              @SessionAttribute SessionUser sessionUser,
                                              HttpSession httpSession) {
        Long userId = sessionUser.id();
        linkedCollectionService.remove(id, userId);
        Long newCount = linkedCollectionService.getCount(userId);
        updateBadgeInSession(httpSession, newCount);
        String message = messageUtils.getSuccess(SuccessCode.REMOVED);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(SuccessCode.REMOVED.name())
                .message(message)
                .data(Map.of("count", newCount))
                .build());
    }

    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<ApiResponse> clear(@SessionAttribute SessionUser sessionUser,
                                             HttpSession httpSession) {
        Long userId = sessionUser.id();
        linkedCollectionService.removeAll(userId);
        updateBadgeInSession(httpSession, 0L);
        String message = messageUtils.getSuccess(SuccessCode.CLEARED);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(SuccessCode.CLEARED.name())
                .message(message)
                .data(Map.of("count", 0))
                .build());
    }

    private void updateBadgeInSession(HttpSession httpSession, Long newCount) {
        BadgeCountsDTO current = (BadgeCountsDTO) httpSession.getAttribute("badges");
        if (current == null) {
            current = BadgeCountsDTO.builder().build();
        }
        BadgeCountsDTO updated = linkedCollectionService.updateBadge(current, newCount);
        httpSession.setAttribute("badges", updated);
    }
}