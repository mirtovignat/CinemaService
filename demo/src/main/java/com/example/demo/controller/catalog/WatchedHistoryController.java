package com.example.demo.controller.catalog;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.cursor.context.UserCursorContext;
import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.dto.catalog.WatchedSuggestionDTO;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.response.WatchedMovieResponse;
import com.example.demo.dto.response.WatchedPageResponse;
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
        CursorPageResponse<LibrarianMovieViewDTO> cursorPageResponse = watchedHistoryCursorService.page(
                cursorPageRequest.cursor(),
                cursorPageRequest.direction(),
                cursorPageRequest.query(),
                UserCursorContext.builder()
                        .userId(userId)
                        .build()
        );

        WatchedPageResponse<LibrarianMovieViewDTO> watchedPageResponse = WatchedPageResponse.<LibrarianMovieViewDTO>builder()
                .cursorPageResponse(cursorPageResponse)
                .userSidebarViewDTO(userMapper.toSidebarView(userQueryService.getUser(userId)))
                .genres(movieFilterMetaService.getAllGenres())
                .query(cursorPageRequest.query())
                .highlightHistoryId(highlightHistoryId)
                .saveHistory(watchedHistoryService.isHistorySaved(userId))
                .build();

        model.addAttribute("watchedPageResponse", watchedPageResponse);
        return "account/watched";
    }

    @GetMapping("/more")
    @ResponseBody
    public CursorPageResponse<LibrarianMovieViewDTO> more(@ModelAttribute CursorPageRequest cursorPageRequest,
                                                          @CurrentUserId Long userId) {
        return watchedHistoryCursorService.page(
                cursorPageRequest.cursor(),
                cursorPageRequest.direction(),
                cursorPageRequest.query(),
                UserCursorContext.builder()
                        .userId(userId)
                        .build()
        );
    }

    @GetMapping("/suggestions")
    @ResponseBody
    public List<WatchedSuggestionDTO> suggestions(@ModelAttribute CursorPageRequest cursorPageRequest,
                                                  @CurrentUserId(required = false) Long userId) {
        return suggestionService.suggest(
                userId,
                cursorPageRequest.query(),
                (uId, normalizedQuery) -> watchedHistoryCursorService
                        .findGroupedWatchedSuggestionsStartingWith(
                                uId,
                                normalizedQuery,
                                PageRequest.of(0, 10)
                        )
                        .stream()
                        .map(projection -> WatchedSuggestionDTO.builder()
                                .movieId(projection.getMovieId())
                                .title(projection.getTitle())
                                .watchCount(projection.getWatchCount())
                                .lastWatchedAt(projection.getLastWatchedAt())
                                .build())
                        .toList());
    }

    @PostMapping("/add/{id}")
    @ResponseBody
    public ResponseEntity<WatchedMovieResponse> watch(@PathVariable Long id,
                                                      @CurrentUserId Long userId) {
        boolean saveHistory = watchedHistoryService.isHistorySaved(userId);

        // Если история выключена, нам всё равно нужно проверить, куплен ли фильм.
        // Если включена - watch() сам проверит это и выбросит ошибку.
        if (!saveHistory) {
            watchedHistoryService.ensurePurchased(id, userId);
        }

        Long historyItemId = saveHistory ? watchedHistoryService.watch(id, userId) : null;

        String message = saveHistory
                ? messageUtils.getSuccess(SuccessCode.WATCHED)
                : messageUtils.getSuccess(SuccessCode.HISTORY_DISABLED_WARNING);

        return ResponseEntity.ok(WatchedMovieResponse.builder()
                .saved(saveHistory)
                .message(message)
                .historyItemId(historyItemId)
                .build());
    }

    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> clear(@CurrentUserId Long userId) {
        watchedHistoryService.clear(userId);
        return apiResponseFactory.ok(SuccessCode.CLEARED);
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Long id,
                                                    @CurrentUserId Long userId) {
        watchedHistoryService.removeHistoryItem(userId, id);
        return apiResponseFactory.ok(SuccessCode.REMOVED);
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> setWatchedHistory(@RequestParam boolean save,
                                                               @CurrentUserId Long userId) {
        watchedHistoryService.setWatchedHistory(userId, save);
        SuccessCode successCode = save ? SuccessCode.HISTORY_ENABLED : SuccessCode.HISTORY_DISABLED;
        return apiResponseFactory.ok(successCode);
    }
}