package com.example.demo.controller.catalog;

import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.cursor.AbstractUserCursorService;
import com.example.demo.service.cursor.util.UserCursorContext;
import com.example.demo.service.movie.FilterMetaService;
import com.example.demo.service.user.UserQueryService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

public abstract class AbstractCatalogPageController<D> {

    protected final AbstractUserCursorService<?, D> cursorService;
    protected final FilterMetaService filterMetaService;
    protected final UserQueryService userQueryService;
    protected final UserMapper userMapper;
    protected final String viewName;

    protected AbstractCatalogPageController(AbstractUserCursorService<?, D> cursorService,
                                            FilterMetaService filterMetaService,
                                            UserQueryService userQueryService,
                                            UserMapper userMapper,
                                            String viewName) {
        this.cursorService = cursorService;
        this.filterMetaService = filterMetaService;
        this.userQueryService = userQueryService;
        this.userMapper = userMapper;
        this.viewName = viewName;
    }

    @GetMapping
    public String getPage(@RequestParam(required = false) String cursor,
                          @RequestParam(required = false) String sort,
                          SessionUser sessionUser,
                          Model model) {
        Long userId = sessionUser.id();
        
        model.addAttribute("response", cursorService.page(cursor, sort, new UserCursorContext(userId)));
        model.addAttribute("userSidebar", userMapper.toSidebarView(userQueryService.getUser(userId)));
        model.addAttribute("genres", filterMetaService.getAllGenres());
        
        applyAdditionalAttributes(userId, model);
        
        return viewName;
    }

    @GetMapping("/more")
    @ResponseBody
    public CursorPageResponse<D> more(@RequestParam(required = false) String cursor,
                                      @RequestParam(required = false) String sort,
                                      SessionUser sessionUser) {
        return cursorService.page(cursor, sort, new UserCursorContext(sessionUser.id()));
    }

    protected void applyAdditionalAttributes(Long userId, Model model) {
        // По умолчанию ничего не делаем. Watched переопределит это.
    }
}