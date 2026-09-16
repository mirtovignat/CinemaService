package com.example.demo.controller;

import com.example.demo.dto.base.MovieIdentity;
import com.example.demo.dto.movie.MovieCardDetailsViewDTO;
import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieForUser;
import com.example.demo.dto.movie.MovieSearchDTO;
import com.example.demo.dto.response.GenreMoviesPageResponse;
import com.example.demo.dto.response.MoviesPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.filter.MovieFilter;
import com.example.demo.model.Genre;
import com.example.demo.service.movie.MovieRecommendationService;
import com.example.demo.service.movie.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final MovieRecommendationService movieRecommendationService;
    private static final int RECOMMENDATIONS_PAGE_SIZE = 8;

    @GetMapping({"/", "/movies"})
    public String showMovies(
            @Valid @ModelAttribute MovieFilter movieFilter,
            @PageableDefault(size = 15, sort = {"releaseDate", "id"}, direction = Sort.Direction.DESC) Pageable pageable,
            @SessionAttribute(required = false) SessionUser sessionUser,
            Model model
    ) {
        Long userId = sessionUser != null ? sessionUser.id() : null;
        Page<MovieForUser<MovieCardViewDTO>> cardsPage = movieService.getMovies(movieFilter, pageable, userId);
        if (cardsPage.getTotalElements() == 1) {
            MovieIdentity identity = cardsPage.getContent().get(0).movie().id();
            Long movieId = identity.id();
            return "redirect:/movies/" + movieId;
        }

        boolean hasSearchParams = movieFilter != null && movieFilter.hasAnyCriteria();

        MoviesPageResponse response = MoviesPageResponse.builder()
                .cardsPage(cardsPage)
                .filter(movieFilter)
                .isSearch(hasSearchParams)
                .build();
        model.addAttribute("response", response);
        return "index";
    }

    @GetMapping("/recommendations")
    @ResponseBody
    public Slice<MovieCardViewDTO> getRecommendationsAjax(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return movieRecommendationService.getRecommendedMoviesSlice(
                userId,
                PageRequest.of(page, RECOMMENDATIONS_PAGE_SIZE)
        );
    }

    @GetMapping("/movies/{id}")
    public String showMovie(
            @PathVariable Long id,
            @SessionAttribute(required = false) SessionUser sessionUser,
            Model model
    ) {
        Long userId = sessionUser != null ? sessionUser.id() : null;
        MovieForUser<MovieCardDetailsViewDTO> card = movieService.getCard(id, userId);
        model.addAttribute("card", card);
        return "show";
    }

    @GetMapping(value = "/", params = "genre")
    public String showMoviesByGenre(
            @RequestParam String genre,
            @PageableDefault(size = 15, sort = {"releaseDate", "id"}, direction = Sort.Direction.DESC) Pageable pageable,
            @SessionAttribute(required = false) SessionUser sessionUser,
            Model model
    ) {
        Long userId = sessionUser != null ? sessionUser.id() : null;
        try {
            Genre.fromDisplayName(genre);
        } catch (IllegalArgumentException illegalArgumentException) {
            return "redirect:/";
        }
        Page<MovieForUser<MovieCardViewDTO>> cardsPage = movieService.getMoviesByGenre(genre, pageable, userId);
        if (cardsPage.getTotalElements() == 1) {
            Long movieId = cardsPage.getContent().get(0).movie().id().id();
            return "redirect:/movies/" + movieId;
        }
        GenreMoviesPageResponse response = GenreMoviesPageResponse.builder()
                .cardsPage(cardsPage)
                .genre(genre)
                .isSearch(true)
                .build();
        model.addAttribute("response", response);
        return "index";
    }

    @GetMapping("/movies/{id}/rating")
    @ResponseBody
    public ResponseEntity<Map<String, Double>> getMovieRating(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("rating", movieService.getMovieRating(id)));
    }

    @GetMapping("/movies/suggestions")
    @ResponseBody
    public ResponseEntity<List<MovieSearchDTO>> getSearchSuggestions(@RequestParam String query) {
        return ResponseEntity.ok(movieService.getMovieSearchDTO(query));
    }

    @GetMapping("/directors")
    @ResponseBody
    public ResponseEntity<List<String>> getAllDirectors() {
        return ResponseEntity.ok(movieService.getAllDirectors());
    }

    @GetMapping("/genres")
    @ResponseBody
    public ResponseEntity<List<String>> getAllGenres() {
        return ResponseEntity.ok(movieService.getAllGenres());
    }
}