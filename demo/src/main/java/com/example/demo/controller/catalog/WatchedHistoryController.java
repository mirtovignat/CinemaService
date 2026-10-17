package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.SuccessCode;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.catalog.WatchedHistoryService;
import com.example.demo.service.catalog.cursor.WatchedHistoryCursorService;
import com.example.demo.service.movie.FilterMetaService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/watched")
@RequiredArgsConstructor
public class WatchedHistoryController {

    private final WatchedHistoryService watchedHistoryService;
    private final WatchedHistoryCursorService watchedHistoryCursorService;
    private final MessageUtils messageUtils;
    private final FilterMetaService filterMetaService;
    private final UserQueryService userQueryService;
    private final UserMapper userMapper;

    @GetMapping
    public String getWatched(@RequestParam(required = false) String cursor,
                             @RequestParam(required = false) String sort,
                             @RequestParam(required = false) Integer size,
                             SessionUser sessionUser,
                             Model model) {
        Long userId = sessionUser.id();
        model.addAttribute("response",
                watchedHistoryCursorService.page(cursor, sort, size, userId));
        model.addAttribute("userSidebar",
                userMapper.toSidebarView(userQueryService.getUser(userId)));
        model.addAttribute("genres", filterMetaService.getAllGenres());
        model.addAttribute("saveHistory", watchedHistoryService.isHistorySaved(userId));
        return "catalog/watched";
    }

    @GetMapping("/more")
    @ResponseBody
    public CursorPageResponse<LibrarianMovieViewDTO> more(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer size,
            SessionUser sessionUser) {
        return watchedHistoryCursorService.page(cursor, sort, size, sessionUser.id());
    }

    @PostMapping("/add/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> watch(@PathVariable Long id,
                                                     SessionUser sessionUser) {
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
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.CLEARED,
                messageUtils.getSuccess(SuccessCode.CLEARED)
        ));
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> remove(@PathVariable Long id,
                                              SessionUser sessionUser) {
        watchedHistoryService.removeHistoryItem(sessionUser.id(), id);
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.REMOVED,
                messageUtils.getSuccess(SuccessCode.REMOVED)
        ));
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<ApiResponse> setWatchedHistory(@RequestParam boolean save,
                                                         SessionUser sessionUser) {
        watchedHistoryService.setWatchedHistory(sessionUser.id(), save);
        SuccessCode code = save ? SuccessCode.HISTORY_ENABLED : SuccessCode.HISTORY_DISABLED;
        return ResponseEntity.ok(ApiResponse.success(code, messageUtils.getSuccess(code)));
    }
}