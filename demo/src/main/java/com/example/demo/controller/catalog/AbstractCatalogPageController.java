package com.example.demo.controller.catalog;

import com.example.demo.cursor.context.UserCursorContext;
import com.example.demo.dto.request.CursorPageRequest;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.response.UserCatalogPageResponse;
import com.example.demo.dto.user.UserSidebarViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.cursor.CursorPageService;
import com.example.demo.service.cursor.SuggestableCursorService;
import com.example.demo.service.cursor.SuggestionService;
import com.example.demo.service.movie.MovieFilterMetaService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.web.resolver.CurrentUserId;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@AllArgsConstructor
public abstract class AbstractCatalogPageController<DTO> {

    protected final CursorPageService<UserCursorContext, DTO> cursorPageService;
    protected final MovieFilterMetaService movieFilterMetaService;
    protected final UserQueryService userQueryService;
    protected final UserMapper userMapper;
    protected final SuggestionService suggestionService;
    protected final String viewName;
    protected final String pageAttributeName;

    @GetMapping
    public String getPage(@ModelAttribute CursorPageRequest cursorPageRequest,
                          @CurrentUserId Long userId,
                          Model model) {
        UserCatalogPageResponse<DTO> userCatalogPageResponse = buildPageResponse(cursorPageRequest, userId);
        model.addAttribute(pageAttributeName, userCatalogPageResponse);
        return viewName;
    }

    private UserCatalogPageResponse<DTO> buildPageResponse(CursorPageRequest cursorPageRequest, Long userId) {
        CursorPageResponse<DTO> cursorPageResponse = cursorPageService.page(
                cursorPageRequest.cursor(),
                cursorPageRequest.direction(),
                cursorPageRequest.searchQuery(),
                UserCursorContext.builder()
                        .userId(userId)
                        .build()
        );

        UserSidebarViewDTO userSidebarViewDTO = userMapper.toSidebarView(userQueryService.getUser(userId));
        List<String> genres = movieFilterMetaService.getAllGenres();

        return UserCatalogPageResponse.of(
                cursorPageResponse,
                userSidebarViewDTO,
                genres,
                cursorPageRequest.searchQuery()
        );
    }

    @GetMapping("/more")
    @ResponseBody
    public CursorPageResponse<DTO> more(@ModelAttribute CursorPageRequest cursorPageRequest,
                                        @CurrentUserId Long userId) {
        return cursorPageService.page(
                cursorPageRequest.cursor(),
                cursorPageRequest.direction(),
                cursorPageRequest.searchQuery(),
                UserCursorContext.builder()
                        .userId(userId)
                        .build()
        );
    }

    @GetMapping("/suggestions")
    @ResponseBody
    public List<String> suggestions(@ModelAttribute CursorPageRequest cursorPageRequest,
                                    @CurrentUserId(required = false) Long userId) {
        if (!(cursorPageService instanceof SuggestableCursorService suggestableCursorService)) {
            return List.of();
        }
        return suggestionService.suggest(
                userId,
                cursorPageRequest.searchQuery(),
                (currentUserId, normalizedQuery, limit) -> suggestableCursorService.findTitlesStartingWith(
                        currentUserId,
                        normalizedQuery,
                        PageRequest.of(0, limit)
                )
        );
    }
}