package com.example.demo.web.resolver;

import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieForUser;
import com.example.demo.filter.PageRequestContext;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class MovieSingleResultResolver {

    public String resolveRedirect(Page<MovieForUser<MovieCardViewDTO>> moviePage,
                                  boolean hasActiveCriteria,
                                  PageRequestContext pageRequestContext) {
        if (!hasActiveCriteria) {
            return null;
        }
        if (moviePage.getTotalElements() != 1 || moviePage.getContent().isEmpty()) {
            return null;
        }
        if (pageRequestContext.getSafePage() != 0) {
            return null;
        }
        if (moviePage.getPageable().getPageSize() <= 1) {
            return null;
        }

        MovieCardViewDTO singleMovie = moviePage.getContent().get(0).movie();
        return "redirect:/movies/" + singleMovie.id();
    }
}