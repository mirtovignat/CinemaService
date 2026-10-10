package com.example.demo.controller;

import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieDetailsViewDTO;
import com.example.demo.dto.movie.MovieForUser;
import com.example.demo.dto.movie.MovieSearchDTO;
import com.example.demo.dto.response.GenreMoviesPageResponse;
import com.example.demo.dto.response.MoviesPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.filter.MovieFilter;
import com.example.demo.service.movie.FilterMetaService;
import com.example.demo.service.movie.MovieRecommendationService;
import com.example.demo.service.movie.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
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
    private final FilterMetaService filterMetaService;

    @GetMapping({"/", "/movies"})
    public String showMovies(
            @RequestParam(value = "title", required = false) String title,
            @Valid @ModelAttribute MovieFilter movieFilter,
            @PageableDefault(size = 15, sort = {"releaseDate", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable,
            SessionUser sessionUser,
            Model model
    ) {
        Long userId = sessionUser != null ? sessionUser.id() : null;
        Page<MovieForUser<MovieCardViewDTO>> cardsPage =
                movieService.getMovies(movieFilter, title, pageable, userId);

        String redirect = redirectIfSingle(cardsPage);
        if (redirect != null) {
            return redirect;
        }

        boolean isSearch = (title != null && !title.isBlank())
                || (movieFilter != null && movieFilter.hasAnyCriteria());

        MoviesPageResponse response = MoviesPageResponse.builder()
                .cardsPage(cardsPage)
                .filter(movieFilter)
                .isSearch(isSearch)
                .build();
        model.addAttribute("response", response);
        return "index";
    }

    @GetMapping("/recommendations")
    @ResponseBody
    public Slice<MovieCardViewDTO> getRecommendationsAjax(
            @RequestParam(defaultValue = "0") int page,
            SessionUser sessionUser
    ) {
        return movieRecommendationService.getRecommendedMoviesSlice(sessionUser.id(), page);
    }

    @GetMapping("/movies/{id}")
    public String showMovie(
            @PathVariable Long id,
            SessionUser sessionUser,
            Model model
    ) {
        Long userId = sessionUser != null ? sessionUser.id() : null;
        MovieForUser<MovieDetailsViewDTO> card = movieService.getCard(id, userId);
        model.addAttribute("card", card);
        return "show";
    }

    @GetMapping(value = "/movies", params = "genre")
    public String showMoviesByGenre(
            @RequestParam String genre,
            @PageableDefault(size = 15, sort = {"releaseDate", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable,
            SessionUser sessionUser,
            Model model
    ) {
        Long userId = sessionUser != null ? sessionUser.id() : null;

        if (genre == null || genre.isBlank() || !filterMetaService.getAllGenres().contains(genre)) {
            return "redirect:/";
        }

        Page<MovieForUser<MovieCardViewDTO>> cardsPage = movieService.getMoviesByGenre(genre, pageable, userId);

        String redirect = redirectIfSingle(cardsPage);
        if (redirect != null) {
            return redirect;
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
    public ResponseEntity<Map<String, Object>> getMovieRating(@PathVariable Long id) {
        Double rating = movieService.getMovieRating(id);
        return ResponseEntity.ok(Map.of("rating", rating != null ? rating : "-"));
    }

    @GetMapping("/movies/suggestions")
    @ResponseBody
    public ResponseEntity<List<MovieSearchDTO>> getSearchSuggestions(@RequestParam String query) {
        return ResponseEntity.ok(movieService.getMovieSearchDTO(query));
    }

    private String redirectIfSingle(Page<MovieForUser<MovieCardViewDTO>> cardsPage) {
        if (cardsPage.getTotalElements() != 1) {
            return null;
        }
        Long movieId = cardsPage.getContent().get(0).movie().id();
        return "redirect:/movies/" + movieId;
    }
}