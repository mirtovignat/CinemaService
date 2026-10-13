package com.example.demo.controller.catalog;

import com.example.demo.dto.response.GroupedHistoryResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.service.movie.FilterMetaService;
import lombok.AllArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.function.Function;

@AllArgsConstructor
public abstract class AbstractGroupedHistoryController<DTO> {

    protected final String viewName;
    protected final FilterMetaService filterMetaService;
    private final Function<Long, GroupedHistoryResponse<DTO>> groupedHistoryProvider;


    @GetMapping
    public String getCatalog(Model model, SessionUser sessionUser) {
        model.addAttribute("groupedResponse", groupedHistoryProvider.apply(sessionUser.id()));
        model.addAttribute("genres", filterMetaService.getAllGenres());
        return viewName;
    }
}