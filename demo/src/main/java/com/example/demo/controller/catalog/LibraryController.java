package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.dto.response.UserCursorPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.service.catalog.LibraryService;
import com.example.demo.service.movie.FilterMetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/library")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;
    private final FilterMetaService filterMetaService;

    @GetMapping
    public String getLibrary(@RequestParam(required = false) String cursor,
                             @RequestParam(required = false) String sort,
                             @RequestParam(required = false) Integer size,
                             SessionUser sessionUser,
                             Model model) {
        model.addAttribute("response",
                libraryService.getPageWithSidebar(sessionUser.id(), cursor, sort, size));
        model.addAttribute("genres", filterMetaService.getAllGenres());
        return "catalog/library";
    }

    @GetMapping("/more")
    @ResponseBody
    public UserCursorPageResponse<LibrarianMovieViewDTO> more(@RequestParam(required = false) String cursor,
                                                              @RequestParam(required = false) String sort,
                                                              @RequestParam(required = false) Integer size,
                                                              SessionUser sessionUser) {
        return libraryService.getPageWithSidebar(sessionUser.id(), cursor, sort, size);
    }
}