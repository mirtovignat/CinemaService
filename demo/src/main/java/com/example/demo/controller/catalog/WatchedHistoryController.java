package com.example.demo.controller.catalog;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.cursor.context.UserCursorContext;
import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.dto.catalog.WatchedSuggestionDTO;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.response.UserCursorPageResponse;
import com.example.demo.dto.user.UserSidebarViewDTO;
import com.example.demo.exception.SuccessCode;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.catalog.WatchedHistoryService;
import com.example.demo.service.cursor.SuggestionService;
import com.example.demo.service.cursor.WatchedHistoryCursorService;
import com.example.demo.service.movie.MovieFilterMetaService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.resolver.CurrentUserId;
import com.example.demo.web.util.ApiResponseFactory;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Controller
@RequestMapping("/watched")
public class WatchedHistoryController {

    private final WatchedHistoryCursorService watchedHistoryCursorService;
    private final WatchedHistoryService watchedHistoryService;
    private final UserQueryService userQueryService;
    private final UserMapper userMapper;
    private final MovieFilterMetaService movieFilterMetaService;
    private final MessageUtils messageUtils;
    private final ApiResponseFactory apiResponseFactory;
    private final SuggestionService suggestionService;

    @GetMapping
    public String getPage(@ModelAttribute CursorPageRequest cursorPageRequest,
                          @RequestParam(value = "highlight", required = false) Long highlightHistoryId,
                          @CurrentUserId Long userId,
                          Model model) {
        model.addAttribute("query", cursorPageRequest.query());
        model.addAttribute("highlightHistoryId", highlightHistoryId);
        model.addAttribute("response",
                watchedHistoryCursorService.page(
                        cursorPageRequest.cursor(),
                        cursorPageRequest.direction(),
                        cursorPageRequest.query(),
                        UserCursorContext.builder()
                                .userId(userId)
                                .build()));
        model.addAttribute("userSidebarViewDTO", userMapper.toSidebarView(userQueryService.getUser(userId)));
        model.addAttribute("genres", movieFilterMetaService.getAllGenres());
        model.addAttribute("saveHistory", watchedHistoryService.isHistorySaved(userId));
        return "account/watched";
    }

    @GetMapping("/more")
    @ResponseBody
    public UserCursorPageResponse<LibrarianMovieViewDTO> more(@ModelAttribute CursorPageRequest cursorPageRequest,
                                                              @CurrentUserId Long userId) {
        CursorPageResponse<LibrarianMovieViewDTO> cursorPageResponse = watchedHistoryCursorService.page(
                cursorPageRequest.cursor(),
                cursorPageRequest.direction(),
                cursorPageRequest.query(),
                UserCursorContext.builder()
                        .userId(userId)
                        .build());
        UserSidebarViewDTO userSidebarViewDTO = userMapper.toSidebarView(userQueryService.getUser(userId));
        return UserCursorPageResponse.<LibrarianMovieViewDTO>builder()
                .cursorPageResponse(cursorPageResponse)
                .userSidebarViewDTO(userSidebarViewDTO)
                .build();
    }

    @GetMapping("/suggestions")
    @ResponseBody
    public List<WatchedSuggestionDTO> suggestions(@RequestParam(value = "query", required = false, defaultValue = "") String query,
                                                  @CurrentUserId(required = false) Long userId) {
        return suggestionService.suggest(userId, query,
                (uId, normalizedQuery) -> watchedHistoryCursorService
                        .findGroupedWatchedSuggestionsStartingWith(uId, normalizedQuery, PageRequest.of(0, 10))
                        .stream()
                        .map(projection -> new WatchedSuggestionDTO(
                                projection.getMovieId(),
                                projection.getTitle(),
                                projection.getWatchCount(),
                                projection.getLastWatchedAt()
                        ))
                        .toList());
    }

    @PostMapping("/add/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> watch(@PathVariable Long id,
                                                     @CurrentUserId Long userId) {
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
    public ResponseEntity<ApiResponse> clear(@CurrentUserId Long userId) {
        watchedHistoryService.clear(userId);
        return apiResponseFactory.ok(SuccessCode.CLEARED);
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> remove(@PathVariable Long id,
                                              @CurrentUserId Long userId) {
        watchedHistoryService.removeHistoryItem(userId, id);
        return apiResponseFactory.ok(SuccessCode.REMOVED);
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<ApiResponse> setWatchedHistory(@RequestParam boolean save,
                                                         @CurrentUserId Long userId) {
        watchedHistoryService.setWatchedHistory(userId, save);
        SuccessCode successCode = save ? SuccessCode.HISTORY_ENABLED : SuccessCode.HISTORY_DISABLED;
        return apiResponseFactory.ok(successCode);
    }
}