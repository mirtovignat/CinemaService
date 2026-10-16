package com.example.demo.controller.catalog;

import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.response.UserCursorPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.service.cursor.AbstractCursorService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@AllArgsConstructor
public abstract class AbstractCursorController<DTO> {

    protected final String viewName;
    protected final AbstractCursorService<?, DTO> cursorService;
    protected final boolean withSidebar;

    @GetMapping
    public String getList(Model model,
                          SessionUser sessionUser,
                          @RequestParam(value = "sort", required = false) String sortDirection,
                          @RequestParam(value = "cursor", required = false) String rawCursor,
                          @RequestParam(value = "size", required = false) Integer requestedPageSize) {
        Long userId = sessionUser.id();
        if (withSidebar) {
            UserCursorPageResponse<DTO> response = cursorService.getPageWithSidebar(
                    userId, rawCursor, sortDirection, requestedPageSize);
            model.addAttribute("response", response);
        } else {
            CursorPageResponse<DTO> response = cursorService.getPage(
                    userId, rawCursor, sortDirection, requestedPageSize);
            model.addAttribute("response", response);
        }
        return viewName;
    }

    @GetMapping("/more")
    @ResponseBody
    public ResponseEntity<CursorPageResponse<DTO>> getMore(
            SessionUser sessionUser,
            @RequestParam(value = "sort", required = false) String sortDirection,
            @RequestParam(value = "cursor", required = false) String rawCursor,
            @RequestParam(value = "size", required = false) Integer requestedPageSize) {
        CursorPageResponse<DTO> response = cursorService.getPage(
                sessionUser.id(), rawCursor, sortDirection, requestedPageSize);
        return ResponseEntity.ok(response);
    }
}