package com.example.demo.controller.catalog;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.cursor.context.UserCursorContext;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.response.UserCursorPageResponse;
import com.example.demo.dto.user.UserSidebarViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.cursor.CursorPageService;
import com.example.demo.service.movie.MovieFilterMetaService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.web.resolver.CurrentUserId;
import lombok.AllArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;

@AllArgsConstructor
public abstract class AbstractCatalogPageController<DTO> {

    protected final CursorPageService<UserCursorContext, DTO> cursorService;
    protected final MovieFilterMetaService movieFilterMetaService;
    protected final UserQueryService userQueryService;
    protected final UserMapper userMapper;
    protected final String viewName;

    @GetMapping
    public String getPage(@ModelAttribute CursorPageRequest cursorPageRequest,
                          @CurrentUserId Long userId,
                          Model model) {
        model.addAttribute("query", cursorPageRequest.query());
        model.addAttribute("response",
                cursorService.page(
                        cursorPageRequest.cursor(),
                        cursorPageRequest.direction(),
                        cursorPageRequest.query(),
                        UserCursorContext.builder()
                                .userId(userId)
                                .build()
                )
        );
        model.addAttribute("userSidebarViewDTO",
                userMapper.toSidebarView(userQueryService.getUser(userId)));
        model.addAttribute("genres",
                movieFilterMetaService.getAllGenres());
        return viewName;
    }

    @GetMapping("/more")
    @ResponseBody
    public UserCursorPageResponse<DTO> more(@ModelAttribute CursorPageRequest cursorPageRequest,
                                            @CurrentUserId Long userId) {
        CursorPageResponse<DTO> cursorPageResponse = cursorService.page(
                cursorPageRequest.cursor(),
                cursorPageRequest.direction(),
                cursorPageRequest.query(),
                UserCursorContext.builder()
                        .userId(userId)
                        .build()
        );
        UserSidebarViewDTO userSidebarViewDTO = userMapper.toSidebarView(userQueryService.getUser(userId));
        return UserCursorPageResponse.<DTO>builder()
                .cursorPageResponse(cursorPageResponse)
                .userSidebarViewDTO(userSidebarViewDTO)
                .build();
    }
}