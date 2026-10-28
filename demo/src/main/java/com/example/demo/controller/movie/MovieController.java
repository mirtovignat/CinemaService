package com.example.demo.controller.movie;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.cursor.SortDirection;
import com.example.demo.cursor.context.RecommendationCursorContext;
import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieDetailsViewDTO;
import com.example.demo.dto.movie.MovieForUser;
import com.example.demo.dto.movie.MovieSearchDTO;
import com.example.demo.dto.movie.tmdb.TmdbCreateRequestDTO;
import com.example.demo.dto.movie.tmdb.TmdbSearchRequest;
import com.example.demo.dto.movie.tmdb.TmdbSearchResultDTO;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

        Page<MovieForUser<MovieCardViewDTO>> moviePage = movieFilterFacade.filterMovies(
                movieFilter, title, pageRequestContext, userId
        );

        boolean hasActiveCriteria = isSearch(title, movieFilter);
        String redirect = redirectIfSingle(moviePage, hasActiveCriteria, pageRequestContext, title);
        if (redirect != null) {
            return redirect;
        }

        MoviesPageResponse moviesPageResponse = MoviesPageResponse.builder()
                .cardsPage(moviePage)
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
        Page<MovieForUser<MovieCardViewDTO>> moviePage = movieFilterFacade.filterByGenre(
                genre, pageRequestContext, userId
        );

        String redirect = redirectIfSingle(moviePage, true, pageRequestContext, null);
        if (redirect != null) {
            return redirect;
        }

        GenreMoviesPageResponse genreMoviesPageResponse = GenreMoviesPageResponse.builder()
                .cardsPage(moviePage)
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
        MovieForUser<MovieDetailsViewDTO> movieForUser = movieService.getCard(id, userId);
        model.addAttribute("card", movieForUser);
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

    @GetMapping("/movies/new")
    public String showCreateMovieForm() {
        return "catalog/create-movie";
    }

    @GetMapping("/movies/tmdb/search")
    @ResponseBody
    public ResponseEntity<List<TmdbSearchResultDTO>> searchTmdb(
            @RequestParam(required = false) List<Long> tmdbIds,
            @RequestParam(required = false) List<String> titles,
            @RequestParam(required = false) List<String> directors,
            @RequestParam(required = false) List<String> actors
    ) {
        TmdbSearchRequest tmdbSearchRequest = TmdbSearchRequest.builder()
                .tmdbIds(tmdbIds)
                .titles(titles)
                .directors(directors)
                .actors(actors)
                .build();
        return ResponseEntity.ok(movieService.searchTmdb(tmdbSearchRequest));
    }

    @PostMapping("/movies/tmdb/create")
    public String createFromTmdb(
            @Valid @ModelAttribute TmdbCreateRequestDTO tmdbCreateRequestDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getFieldErrors().isEmpty()
                            ? null
                            : bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return "redirect:/movies/new";
        }

        List<MovieDetailsViewDTO> createdMovieDetails = movieService.createMovies(
                tmdbCreateRequestDTO.moviePriceItems()
        );

        redirectAttributes.addFlashAttribute("successMessage",
                "Создано фильмов: " + createdMovieDetails.size());
        return "redirect:/movies";
    }

    private boolean isSearch(String title, MovieFilter movieFilter) {
        return (title != null && !title.isBlank())
                || (movieFilter != null && movieFilter.hasAnyCriteria());
    }

    private String redirectIfSingle(
            Page<MovieForUser<MovieCardViewDTO>> moviePage,
            boolean hasActiveCriteria,
            PageRequestContext pageRequestContext,
            String searchedTitle
    ) {
        if (!hasActiveCriteria) return null;
        if (moviePage.getTotalElements() != 1 || moviePage.getContent().isEmpty()) return null;
        if (pageRequestContext.getSafePage() != 0) return null;
        if (moviePage.getPageable().getPageSize() <= 1) return null;

        MovieCardViewDTO singleMovie = moviePage.getContent().get(0).movie();

        if (searchedTitle != null && !searchedTitle.isBlank()) {
            String normalizedSearched = searchedTitle.trim().toLowerCase().replace('ё', 'е');
            String normalizedMovieTitle = singleMovie.title().trim().toLowerCase().replace('ё', 'е');
            if (normalizedSearched.equals(normalizedMovieTitle)) {
                return null;
            }
        }

        return "redirect:/movies/" + singleMovie.id();
    }
}