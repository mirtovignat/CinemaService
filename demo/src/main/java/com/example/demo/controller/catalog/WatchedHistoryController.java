package com.example.demo.controller.catalog;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.cursor.context.UserCursorContext;
import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.response.UserCursorPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.dto.user.UserSidebarViewDTO;
import com.example.demo.exception.SuccessCode;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.catalog.WatchedHistoryService;
import com.example.demo.service.cursor.WatchedHistoryCursorService;
import com.example.demo.service.movie.FilterMetaService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.util.ApiResponseFactory;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@AllArgsConstructor
@Controller
@RequestMapping("/watched")
public class WatchedHistoryController {

    private final WatchedHistoryCursorService watchedHistoryCursorService;
    private final WatchedHistoryService watchedHistoryService;
    private final UserQueryService userQueryService;
    private final UserMapper userMapper;
    private final FilterMetaService filterMetaService;
    private final MessageUtils messageUtils;
    private final ApiResponseFactory apiResponseFactory;

    @GetMapping
    public String getPage(@ModelAttribute CursorPageRequest cursorPageRequest,
                          SessionUser sessionUser,
                          Model model) {
        Long userId = sessionUser.id();
        model.addAttribute("response",
                watchedHistoryCursorService.page(
                        cursorPageRequest.cursor(),
                        cursorPageRequest.direction(),
                        UserCursorContext.builder()
                                .userId(userId)
                                .build()));
        model.addAttribute("userSidebarViewDTO", userMapper.toSidebarView(userQueryService.getUser(userId)));
        model.addAttribute("genres", filterMetaService.getAllGenres());
        model.addAttribute("saveHistory", watchedHistoryService.isHistorySaved(userId));
        return "account/watched";
    }

    @GetMapping("/more")
    @ResponseBody
    public UserCursorPageResponse<LibrarianMovieViewDTO> more(@ModelAttribute CursorPageRequest cursorPageRequest,
                                                              SessionUser sessionUser) {
        Long userId = sessionUser.id();
        CursorPageResponse<LibrarianMovieViewDTO> page = watchedHistoryCursorService.page(
                cursorPageRequest.cursor(),
                cursorPageRequest.direction(),
                UserCursorContext.builder()
                        .userId(userId)
                        .build());
        UserSidebarViewDTO sidebar = userMapper.toSidebarView(userQueryService.getUser(userId));
        return UserCursorPageResponse.<LibrarianMovieViewDTO>builder()
                .cursorPageResponse(page)
                .userSidebarViewDTO(sidebar)
                .build();
    }

    @PostMapping("/add/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> watch(@PathVariable Long id, SessionUser sessionUser) {
        Long userId = sessionUser.id();
        boolean saveHistory = watchedHistoryService.isHistorySaved(userId);
        if (saveHistory) {
            watchedHistoryService.watch(id, userId);
        }
        String message = saveHistory
                ? messageUtils.getSuccess(SuccessCode.WATCHED)
                : "История просмотров отключена";
        return ResponseEntity.ok(Map.of(
                "saved", saveHistory,
                "message", message,
                "success", true
        ));
    }

    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<ApiResponse> clear(SessionUser sessionUser) {
        watchedHistoryService.clear(sessionUser.id());
        return apiResponseFactory.ok(SuccessCode.CLEARED);
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> remove(@PathVariable Long id, SessionUser sessionUser) {
        watchedHistoryService.removeHistoryItem(sessionUser.id(), id);
        return apiResponseFactory.ok(SuccessCode.REMOVED);
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<ApiResponse> setWatchedHistory(@RequestParam boolean save, SessionUser sessionUser) {
        watchedHistoryService.setWatchedHistory(sessionUser.id(), save);
        SuccessCode code = save ? SuccessCode.HISTORY_ENABLED : SuccessCode.HISTORY_DISABLED;
        return apiResponseFactory.ok(code);
    }
}