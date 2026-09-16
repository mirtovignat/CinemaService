package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.GroupedHistoryResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.catalog.WatchedHistoryService;
import com.example.demo.service.movie.MovieService;
import com.example.demo.util.MessageUtils;
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

    private final WatchedHistoryService watchedHistoryService;
    private final MessageUtils messageUtils;
    private final MovieService movieService;

    @GetMapping
    public String getWatchedHistory(
            Model model,
            @SessionAttribute SessionUser sessionUser
    ) {
        Long userId = sessionUser.id();
        GroupedHistoryResponse<LibrarianMovieViewDTO> groupedResponse =
                watchedHistoryService.getGroupedWatchedHistory(userId);
        boolean saveHistory = watchedHistoryService.isHistorySaved(userId);
        model.addAttribute("groupedResponse", groupedResponse);
        model.addAttribute("genres", movieService.getAllGenres());
        model.addAttribute("saveHistory", saveHistory);
        return "catalog/watched";
    }

    @PostMapping("/add/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> watchMovie(
            @PathVariable Long id,
            @SessionAttribute SessionUser sessionUser) {

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
    public ResponseEntity<ApiResponse> clear(
            @SessionAttribute SessionUser sessionUser) {
        Long userId = sessionUser.id();
        watchedHistoryService.clear(userId);
        String message = messageUtils.getSuccess(SuccessCode.CLEARED);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.CLEARED, message));
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> removeHistoryItem(
            @PathVariable Long id,
            @SessionAttribute SessionUser sessionUser) {
        Long userId = sessionUser.id();
        watchedHistoryService.removeHistoryItem(userId, id);
        String message = messageUtils.getSuccess(SuccessCode.REMOVED);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.REMOVED, message));
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<ApiResponse> setWatchedHistory(
            @RequestParam boolean save,
            @SessionAttribute SessionUser sessionUser) {
        Long userId = sessionUser.id();
        watchedHistoryService.setWatchedHistory(userId, save);
        SuccessCode code = save ? SuccessCode.HISTORY_ENABLED : SuccessCode.HISTORY_DISABLED;
        String message = messageUtils.getSuccess(code);
        return ResponseEntity.ok(ApiResponse.success(code, message));
    }
}