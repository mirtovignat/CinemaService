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
import com.example.demo.filter.MovieFilterValidator;
import com.example.demo.filter.PageRequestContext;
import com.example.demo.service.cursor.RecommendationCursorService;
import com.example.demo.service.movie.MovieRecommendationService;
import com.example.demo.service.movie.MovieService;
import com.example.demo.web.resolver.CurrentUserId;
import com.example.demo.web.resolver.MovieSingleResultResolver;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    private final MovieSingleResultResolver movieSingleResultResolver;
    private final MovieFilterValidator movieFilterValidator;

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
            MovieFilter effectiveFilter = movieFilterValidator.orEmpty(movieFilter);
            Page<MovieForUser<MovieCardViewDTO>> emptyPage = Page.empty();
            MoviesPageResponse moviesPageResponse = MoviesPageResponse.of(emptyPage, effectiveFilter, false);
            model.addAttribute("moviesPageResponse", moviesPageResponse);
            model.addAttribute("filterErrors", bindingResult.getAllErrors());
            return "catalog/index";
        }

        Page<MovieForUser<MovieCardViewDTO>> moviePage = movieFilterFacade.filterMovies(
                movieFilter, title, pageRequestContext, userId
        );

        boolean hasActiveCriteria = movieFilterValidator.hasAnySearchCriteria(movieFilter, title);
        String redirect = movieSingleResultResolver.resolveRedirect(moviePage, hasActiveCriteria, pageRequestContext);
        if (redirect != null) {
            return redirect;
        }

        MoviesPageResponse moviesPageResponse = MoviesPageResponse.of(moviePage, movieFilter, hasActiveCriteria);

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
        Page<MovieForUser<MovieCardViewDTO>> moviePage = movieFilterFacade.filterByGenre(
                genre, pageRequestContext, userId
        );

        String redirect = movieSingleResultResolver.resolveRedirect(moviePage, true, pageRequestContext);
        if (redirect != null) {
            return redirect;
        }

        GenreMoviesPageResponse genreMoviesPageResponse = GenreMoviesPageResponse.of(moviePage, genre, true);

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
                        .excludedMovieIds(excludedMovieIds.isEmpty() ? null : excludedMovieIds)
                        .build()
        );
    }

    @GetMapping("/movies/{id}")
    public String showMovie(
            @PathVariable Long id,
            @CurrentUserId(required = false) Long userId,
            Model model
    ) {
        MovieForUser<MovieDetailsViewDTO> movieForUser = movieService.getCard(id, userId);
        model.addAttribute("movieForUser", movieForUser);
        return "catalog/show";
    }

    @GetMapping("/movies/{id}/rating")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMovieRating(@PathVariable Long id) {
        BigDecimal rating = movieService.getMovieRating(id);
        return ResponseEntity.ok(Map.of("rating", rating != null ? rating : "-"));
    }

    @GetMapping("/movies/suggestions")
    @ResponseBody
    public ResponseEntity<List<MovieSearchDTO>> getMovieSearchSuggestions(@RequestParam String query) {
        return ResponseEntity.ok(movieService.getMovieSearchDTO(query));
    }
}