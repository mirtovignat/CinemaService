package com.example.demo.mapper.util;

import com.example.demo.config.TMDBConfig;
import com.example.demo.dto.movie.tmdb.TMDBCast;
import com.example.demo.dto.movie.tmdb.TMDBCredits;
import com.example.demo.dto.movie.tmdb.TMDBCrew;
import com.example.demo.dto.movie.tmdb.TMDBGenre;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TmdbMappingUtil {

    private final TMDBConfig tmdbConfig;

    @Named("posterPathToUrl")
    public String posterPathToUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) {
            return null;
        }
        return tmdbConfig.getImageUrl() + "/" + tmdbConfig.getPosterSize() + posterPath;
    }

    @Named("runtimeToDuration")
    public Duration runtimeToDuration(Integer runtime) {
        return runtime != null ? Duration.ofMinutes(runtime) : Duration.ZERO;
    }

    @Named("allGenreNames")
    public List<String> allGenreNames(List<TMDBGenre> tmdbGenres) {
        if (tmdbGenres == null || tmdbGenres.isEmpty()) {
            return List.of("Другое");
        }
        return tmdbGenres.stream()
                .map(TMDBGenre::name)
                .filter(name -> name != null && !name.isBlank())
                .map(name -> Character.toUpperCase(name.charAt(0)) + name.substring(1))
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
    public List<String> castToActors(TMDBCredits tmdbCredits) {
        if (tmdbCredits == null || tmdbCredits.tmdbCasts() == null) {
            return List.of();
        }
        return tmdbCredits.tmdbCasts().stream()
                .limit(10)
                .map(TMDBCast::name)
                .toList();
    }

    @Named("extractDirector")
    public String extractDirector(TMDBCredits tmdbCredits) {
        if (tmdbCredits == null || tmdbCredits.tmdbCrews() == null) {
            return null;
        }
        return tmdbCredits.tmdbCrews().stream()
                .filter(crew -> "Director".equalsIgnoreCase(crew.job()))
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
            case "18", "NC-17" -> 18;
            default -> 16;
        };
    }
}