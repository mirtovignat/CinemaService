package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.SuccessCode;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.WatchedHistoryService;
import com.example.demo.service.cursor.WatchedHistoryCursorService;
import com.example.demo.service.movie.FilterMetaService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.util.ApiResponseFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/watched")
public class WatchedHistoryController extends AbstractCatalogPageController<LibrarianMovieViewDTO> {

    private final WatchedHistoryService watchedHistoryService;
    private final MessageUtils messageUtils;
    private final ApiResponseFactory apiResponseFactory;

    public WatchedHistoryController(WatchedHistoryCursorService watchedHistoryCursorService,
                                    FilterMetaService filterMetaService,
                                    UserQueryService userQueryService,
                                    UserMapper userMapper,
                                    WatchedHistoryService watchedHistoryService,
                                    MessageUtils messageUtils,
                                    ApiResponseFactory apiResponseFactory) {
        super(watchedHistoryCursorService, filterMetaService, userQueryService, userMapper, "account/watched");
        this.watchedHistoryService = watchedHistoryService;
        this.messageUtils = messageUtils;
        this.apiResponseFactory = apiResponseFactory;
    }

    @Override
    protected void applyAdditionalAttributes(Long userId, Model model) {
        model.addAttribute("saveHistory", watchedHistoryService.isHistorySaved(userId));
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