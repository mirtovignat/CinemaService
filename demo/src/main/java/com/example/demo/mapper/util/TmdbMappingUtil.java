package com.example.demo.mapper.util;

import com.example.demo.dto.movie.tmdb.TMDBCast;
import com.example.demo.dto.movie.tmdb.TMDBCredits;
import com.example.demo.dto.movie.tmdb.TMDBCrew;
import com.example.demo.dto.movie.tmdb.TMDBGenre;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Component
public class TmdbMappingUtil {

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

    @Named("allGenreNames")
    public List<String> allGenreNames(List<TMDBGenre> genres) {
        if (genres == null || genres.isEmpty()) {
            return List.of("Другое");
        }
        return genres.stream()
                .map(TMDBGenre::name)
                .filter(n -> n != null && !n.isBlank())
                .map(n -> Character.toUpperCase(n.charAt(0)) + n.substring(1))
                .toList();
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

    @Named("extractDirector")
    public String extractDirector(TMDBCredits credits) {
        if (credits == null || credits.crew() == null) {
            return null;
        }
        return credits.crew().stream()
                .filter(c -> "Director".equalsIgnoreCase(c.job()))
                .map(TMDBCrew::name)
                .findFirst()
                .orElse(null);
    }

    @Named("certificationToAge")
    public Integer certificationToAge(String certification) {
        if (certification == null || certification.isBlank()) {
            return 16;
        }
        return switch (certification.trim()) {
            case "0", "G" -> 0;
            case "6", "PG" -> 6;
            case "12", "PG-13" -> 12;
            case "16", "R" -> 16;
            case "18", "NC-17" -> 18;
            default -> 16;
        };
    }
}