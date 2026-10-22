package com.example.demo.controller;

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
import com.example.demo.dto.user.SessionUser;
import com.example.demo.filter.MovieFilter;
import com.example.demo.filter.MovieFilterFacade;
import com.example.demo.filter.MovieSearchContext;
import com.example.demo.service.cursor.RecommendationCursorService;
import com.example.demo.service.movie.MovieRecommendationService;
import com.example.demo.service.movie.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final MovieRecommendationService movieRecommendationService;
    private final MovieFilterFacade movieFilterFacade;
    private final RecommendationCursorService recommendationCursorService;

    @GetMapping({"/", "/movies"})
    public String showMovies(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDirection", required = false) String sortDirection,
            @Valid @ModelAttribute MovieFilter movieFilter,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "35") int size,
            SessionUser sessionUser,
            Model model
    ) {
        Long userId = sessionUser.idOrNull();
        MovieSearchContext movieSearchContext = MovieSearchContext.of(title, sortBy, sortDirection);

        MovieFilterFacade.Result result =
                movieFilterFacade.filter(movieFilter, movieSearchContext, page, size, userId);

        String redirect = redirectIfSingle(result.page());
        if (redirect != null) {
            return redirect;
        }

        MoviesPageResponse moviesPageResponse = MoviesPageResponse.builder()
                .cardsPage(result.page())
                .movieFilter(movieFilter)
                .isSearch(isSearch(title, movieFilter))
                .build();

        model.addAttribute("moviesPageResponse", moviesPageResponse);
        model.addAttribute("filterQuery", result.filterQuery());
        return "catalog/index";
    }

    @GetMapping(value = "/movies", params = "genre")
    public String showMoviesByGenre(
            @RequestParam String genre,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDirection", required = false) String sortDirection,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            SessionUser sessionUser,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = sessionUser.idOrNull();

        return movieFilterFacade.filterByGenre(genre, sortBy, sortDirection, page, size, userId)
                .map(result -> {
                    String redirect = redirectIfSingle(result.page());
                    if (redirect != null) {
                        return redirect;
                    }

                    GenreMoviesPageResponse genreMoviesPageResponse = GenreMoviesPageResponse.builder()
                            .cardsPage(result.page())
                            .genre(genre)
                            .isSearch(true)
                            .build();

                    model.addAttribute("genreMoviesPageResponse", genreMoviesPageResponse);
                    model.addAttribute("filterQuery", result.filterQuery());
                    return "catalog/index";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error",
                            "Жанр не найден: " + genre);
                    return "redirect:/";
                });
    }

    @GetMapping("/recommendations")
    @ResponseBody
    public CursorPageResponse<MovieCardViewDTO> getRecommendationsAjax(
            @ModelAttribute CursorPageRequest cursorPageRequest,
            SessionUser sessionUser
    ) {
        Long userId = sessionUser.idOrNull();

        Set<Long> excludedMovieIds = movieRecommendationService.collectExcludedMovieIds(userId);

        return recommendationCursorService.page(
                cursorPageRequest.cursor(),
                SortDirection.DESC,
                RecommendationCursorContext.builder()
                        .userId(userId)
                        .excludedMovieIds(excludedMovieIds.isEmpty() ? null : new ArrayList<>(excludedMovieIds))
                        .build()
        );
    }

    @GetMapping("/movies/{id}")
    public String showMovie(
            @PathVariable Long id,
            SessionUser sessionUser,
            Model model
    ) {
        MovieForUser<MovieDetailsViewDTO> card =
                movieService.getCard(id, sessionUser.idOrNull());
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

    private String redirectIfSingle(Page<MovieForUser<MovieCardViewDTO>> page) {
        if (page.getTotalElements() != 1 || page.getContent().isEmpty()) {
            return null;
        }
        return "redirect:/movies/" + page.getContent().get(0).movie().id();
    }
}