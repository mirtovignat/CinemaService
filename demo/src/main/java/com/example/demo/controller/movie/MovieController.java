package com.example.demo.controller.movie;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.cursor.SortDirection;
import com.example.demo.cursor.context.RecommendationCursorContext;
import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieDetailsViewDTO;
import com.example.demo.dto.movie.MovieForUser;
import com.example.demo.dto.movie.MovieSearchDTO;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.response.GenreMoviesPageResponse;
import com.example.demo.dto.response.MoviesPageResponse;
import com.example.demo.filter.MovieFilter;
import com.example.demo.filter.MovieFilterFacade;
import com.example.demo.filter.PageRequestContext;
import com.example.demo.service.cursor.RecommendationCursorService;
import com.example.demo.service.movie.MovieRecommendationService;
import com.example.demo.service.movie.MovieService;
import com.example.demo.web.resolver.CurrentUserId;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@AllArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final MovieRecommendationService movieRecommendationService;
    private final MovieFilterFacade movieFilterFacade;
    private final RecommendationCursorService recommendationCursorService;

    @GetMapping({"/", "/movies"})
    public String showMovies(
            @RequestParam(value = "title", required = false) String title,
            @Valid @ModelAttribute("movieFilter") MovieFilter movieFilter,
            BindingResult bindingResult,
            @ModelAttribute PageRequestContext pageRequestContext,
            @CurrentUserId(required = false) Long userId,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            Page<MovieForUser<MovieCardViewDTO>> emptyPage = Page.empty();
            MoviesPageResponse moviesPageResponse = MoviesPageResponse.builder()
                    .cardsPage(emptyPage)
                    .movieFilter(movieFilter)
                    .isSearch(false)
                    .build();
            model.addAttribute("moviesPageResponse", moviesPageResponse);
            model.addAttribute("filterErrors", bindingResult.getAllErrors());
            return "catalog/index";
        }

        Page<MovieForUser<MovieCardViewDTO>> result = movieFilterFacade.filterMovies(
                movieFilter, title, pageRequestContext, userId
        );

        boolean hasActiveCriteria = isSearch(title, movieFilter);

        String redirect = redirectIfSingle(result, hasActiveCriteria, pageRequestContext);
        if (redirect != null) {
            return redirect;
        }

        MoviesPageResponse moviesPageResponse = MoviesPageResponse.builder()
                .cardsPage(result)
                .movieFilter(movieFilter)
                .isSearch(hasActiveCriteria)
                .build();

        model.addAttribute("moviesPageResponse", moviesPageResponse);
        return "catalog/index";
    }

    @GetMapping(value = "/movies", params = "genre")
    public String showMoviesByGenre(
            @RequestParam String genre,
            @ModelAttribute PageRequestContext pageRequestContext,
            @CurrentUserId(required = false) Long userId,
            Model model
    ) {
        Page<MovieForUser<MovieCardViewDTO>> result = movieFilterFacade.filterByGenre(
                genre, pageRequestContext, userId
        );

        String redirect = redirectIfSingle(result, true, pageRequestContext);
        if (redirect != null) {
            return redirect;
        }

        GenreMoviesPageResponse genreMoviesPageResponse = GenreMoviesPageResponse.builder()
                .cardsPage(result)
                .genre(genre)
                .isSearch(true)
                .build();

        model.addAttribute("genreMoviesPageResponse", genreMoviesPageResponse);
        return "catalog/index";
    }

    @GetMapping("/recommendations")
    @ResponseBody
    public CursorPageResponse<MovieCardViewDTO> getRecommendationsAjax(
            @ModelAttribute CursorPageRequest cursorPageRequest,
            @CurrentUserId(required = false) Long userId
    ) {
        Set<Long> excludedMovieIds = movieRecommendationService.collectExcludedMovieIds(userId);

        return recommendationCursorService.page(
                cursorPageRequest.cursor(),
                SortDirection.DESC,
                null,
                RecommendationCursorContext.builder()
                        .userId(userId)
                        .excludedMovieIds(excludedMovieIds.isEmpty() ? null : new ArrayList<>(excludedMovieIds))
                        .build()
        );
    }

    @GetMapping("/movies/{id}")
    public String showMovie(
            @PathVariable Long id,
            @CurrentUserId(required = false) Long userId,
            Model model
    ) {
        MovieForUser<MovieDetailsViewDTO> card = movieService.getCard(id, userId);
        model.addAttribute("card", card);
        return "catalog/show";
    }

    @GetMapping("/movies/{id}/rating")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMovieRating(@PathVariable Long id) {
        Double rating = movieService.getMovieRating(id);
        return ResponseEntity.ok(Map.of("rating", rating != null ? rating : "-"));
    }

    @GetMapping("/movies/suggestions")
    @ResponseBody
    public ResponseEntity<List<MovieSearchDTO>> getSearchSuggestions(@RequestParam String query) {
        return ResponseEntity.ok(movieService.getMovieSearchDTO(query));
    }

    private boolean isSearch(String title, MovieFilter movieFilter) {
        return (title != null && !title.isBlank())
                || (movieFilter != null && movieFilter.hasAnyCriteria());
    }

    private String redirectIfSingle(
            Page<MovieForUser<MovieCardViewDTO>> page,
            boolean hasActiveCriteria,
            PageRequestContext pageRequestContext
    ) {
        if (!hasActiveCriteria) {
            return null;
        }
        if (page.getTotalElements() != 1 || page.getContent().isEmpty()) {
            return null;
        }
        if (pageRequestContext.getSafePage() != 0) {
            return null;
        }
        if (page.getPageable().getPageSize() <= 1) {
            return null;
        }
        return "redirect:/movies/" + page.getContent().get(0).movie().id();
    }
}