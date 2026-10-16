package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.dto.response.UserCursorPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.service.catalog.RatedCatalogService;
import com.example.demo.service.movie.FilterMetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/rated/history")
@RequiredArgsConstructor
public class RatedCatalogController {

    private final RatedCatalogService ratedCatalogService;
    private final FilterMetaService filterMetaService;

    @GetMapping
    public String getRatedHistory(@RequestParam(required = false) String cursor,
                                  @RequestParam(required = false) String sort,
                                  @RequestParam(required = false) Integer size,
                                  SessionUser sessionUser,
                                  Model model) {
        model.addAttribute("response",
                ratedCatalogService.getPageWithSidebar(sessionUser.id(), cursor, sort, size));
        model.addAttribute("genres", filterMetaService.getAllGenres());
        return "catalog/rated";
    }

    @GetMapping("/more")
    @ResponseBody
    public UserCursorPageResponse<RatedMovieViewDTO> more(@RequestParam(required = false) String cursor,
                                                          @RequestParam(required = false) String sort,
                                                          @RequestParam(required = false) Integer size,
                                                          SessionUser sessionUser) {
        return ratedCatalogService.getPageWithSidebar(sessionUser.id(), cursor, sort, size);
    }
}