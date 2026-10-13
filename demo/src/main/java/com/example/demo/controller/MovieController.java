package com.example.demo.controller;

import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieDetailsViewDTO;
import com.example.demo.dto.movie.MovieForUser;
import com.example.demo.dto.movie.MovieSearchDTO;
import com.example.demo.dto.response.GenreMoviesPageResponse;
import com.example.demo.dto.response.MoviesPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.filter.MovieFilter;
import com.example.demo.filter.MovieFilterFacade;
import com.example.demo.filter.MovieSearchContext;
import com.example.demo.service.movie.MovieRecommendationService;
import com.example.demo.service.movie.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final MovieRecommendationService movieRecommendationService;
    private final MovieFilterFacade movieFilterFacade;

    @GetMapping({"/", "/movies"})
    public String showMovies(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDir", required = false) String sortDir,
            @Valid @ModelAttribute MovieFilter movieFilter,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "35") int size,
            SessionUser sessionUser,
            Model model
    ) {
        Long userId = userIdOf(sessionUser);
        MovieSearchContext movieSearchContext = MovieSearchContext.of(title, sortBy, sortDir);

        MovieFilterFacade.Result result =
                movieFilterFacade.filter(movieFilter, movieSearchContext, page, size, userId);

        String redirect = redirectIfSingle(result.page());
        if (redirect != null) {
            return redirect;
        }

        MoviesPageResponse response = MoviesPageResponse.builder()
                .cardsPage(result.page())
                .filter(movieFilter)
                .isSearch(isSearch(title, movieFilter))
                .build();

        model.addAttribute("response", response);
        model.addAttribute("filterQuery", result.filterQuery());
        return "index";
    }

    @GetMapping(value = "/movies", params = "genre")
    public String showMoviesByGenre(
            @RequestParam String genre,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDir", required = false) String sortDir,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            SessionUser sessionUser,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = userIdOf(sessionUser);

        return movieFilterFacade.filterByGenre(genre, sortBy, sortDir, page, size, userId)
                .map(result -> {
                    String redirect = redirectIfSingle(result.page());
                    if (redirect != null) {
                        return redirect;
                    }

                    GenreMoviesPageResponse response = GenreMoviesPageResponse.builder()
                            .cardsPage(result.page())
                            .genre(genre)
                            .isSearch(true)
                            .build();

                    model.addAttribute("response", response);
                    model.addAttribute("filterQuery", result.filterQuery());
                    return "index";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error",
                            "Жанр не найден: " + genre);
                    return "redirect:/";
                });
    }

    @GetMapping("/recommendations")
    @ResponseBody
    public Slice<MovieCardViewDTO> getRecommendationsAjax(
            @RequestParam(defaultValue = "0") int page,
            SessionUser sessionUser
    ) {
        return movieRecommendationService.getRecommendedMoviesSlice(userIdOf(sessionUser), page);
    }

    @GetMapping("/movies/{id}")
    public String showMovie(
            @PathVariable Long id,
            SessionUser sessionUser,
            Model model
    ) {
        MovieForUser<MovieDetailsViewDTO> card =
                movieService.getCard(id, userIdOf(sessionUser));
        model.addAttribute("card", card);
        return "show";
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

    private Long userIdOf(SessionUser sessionUser) {
        return sessionUser != null ? sessionUser.id() : null;
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