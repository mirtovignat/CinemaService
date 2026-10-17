package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.catalog.cursor.LibraryCursorService;
import com.example.demo.service.cursor.context.UserCursorContext;
import com.example.demo.service.movie.FilterMetaService;
import com.example.demo.service.user.UserQueryService;
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

    private final LibraryCursorService libraryCursorService;
    private final FilterMetaService filterMetaService;
    private final UserQueryService userQueryService;
    private final UserMapper userMapper;

    @GetMapping
    public String getLibrary(@RequestParam(required = false) String cursor,
                             @RequestParam(required = false) String sort,
                             SessionUser sessionUser,
                             Model model) {
        model.addAttribute("response",
                libraryCursorService.page(cursor, sort, new UserCursorContext(sessionUser.id())));
        model.addAttribute("userSidebar",
                userMapper.toSidebarView(userQueryService.getUser(sessionUser.id())));
        model.addAttribute("genres", filterMetaService.getAllGenres());
        return "catalog/library";
    }

    @GetMapping("/more")
    @ResponseBody
    public CursorPageResponse<LibrarianMovieViewDTO> more(@RequestParam(required = false) String cursor,
                                                          @RequestParam(required = false) String sort,
                                                          SessionUser sessionUser) {
        return libraryCursorService.page(cursor, sort, new UserCursorContext(sessionUser.id()));
    }
}