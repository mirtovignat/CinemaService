package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.HistoricalMovieViewDTO;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.service.catalog.HistoryService;
import com.example.demo.service.movie.FilterMetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;
    private final FilterMetaService filterMetaService;

    @GetMapping
    public String getHistory(@RequestParam(required = false) String cursor,
                             @RequestParam(required = false) String sort,
                             @RequestParam(required = false) Integer size,
                             SessionUser sessionUser,
                             Model model) {
        model.addAttribute("response",
                historyService.getPageWithSidebar(sessionUser.id(), cursor, sort, size));
        model.addAttribute("genres", filterMetaService.getAllGenres());
        return "catalog/history";
    }

    @GetMapping("/more")
    @ResponseBody
    public CursorPageResponse<HistoricalMovieViewDTO> more(@RequestParam(required = false) String cursor,
                                                           @RequestParam(required = false) String sort,
                                                           @RequestParam(required = false) Integer size,
                                                           SessionUser sessionUser) {
        return historyService.getPage(sessionUser.id(), cursor, sort, size);
    }
}