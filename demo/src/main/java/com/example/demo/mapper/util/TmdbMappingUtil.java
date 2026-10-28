package com.example.demo.mapper.util;

import com.example.demo.dto.movie.tmdb.TMDBCast;
import com.example.demo.dto.movie.tmdb.TMDBCredits;
import com.example.demo.dto.movie.tmdb.TMDBGenre;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Component
public class TmdbMappingUtil {

    private static final String DEFAULT_GENRE = "Другое";

    @Named("posterPathToUrl")
    public String posterPathToUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) {
            return null;
        }
        return "https://image.tmdb.org/t/p/w500" + posterPath;
    }

    @Named("runtimeToDuration")
    public Duration runtimeToDuration(Integer runtime) {
        return runtime != null ? Duration.ofMinutes(runtime) : Duration.ZERO;
    }

    @Named("firstGenreName")
    public String firstGenreName(List<TMDBGenre> genres) {
        if (genres == null || genres.isEmpty()) {
            return DEFAULT_GENRE;
        }
        String name = genres.get(0).name();
        if (name == null || name.isBlank()) {
            return DEFAULT_GENRE;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    @Named("parseReleaseDate")
    public LocalDate parseReleaseDate(String releaseDate) {
        if (releaseDate == null || releaseDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(releaseDate);
        } catch (Exception exception) {
            return null;
        }
    }

    @Named("castToActors")
    public List<String> castToActors(TMDBCredits credits) {
        if (credits == null || credits.tmdbCasts() == null) {
            return List.of();
        }
        return credits.tmdbCasts().stream()
                .limit(10)
                .map(TMDBCast::name)
                .toList();
    }
}