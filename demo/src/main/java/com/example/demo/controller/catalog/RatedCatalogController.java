package com.example.demo.controller.catalog;

import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.service.cursor.RatedCursorService;
import com.example.demo.service.movie.FilterMetaService;
import com.example.demo.service.user.UserQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rated/history")
public class RatedCatalogController extends AbstractCatalogPageController<RatedMovieViewDTO> {

    public RatedCatalogController(RatedCursorService ratedCursorService,
                                  FilterMetaService filterMetaService,
                                  UserQueryService userQueryService,
                                  UserMapper userMapper) {
        super(ratedCursorService, filterMetaService, userQueryService, userMapper, "account/rated");
    }
}