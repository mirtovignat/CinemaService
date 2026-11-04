package com.example.demo.service.movie;

import com.example.demo.repository.entity.MovieRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@AllArgsConstructor
@Service
public class MovieRecommendationService {

    private final MovieRepository movieRepository;

    @Transactional(readOnly = true)
    public Set<Long> collectExcludedMovieIds(Long userId) {
        if (userId == null) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(movieRepository.findExcludedMovieIdsByUserId(userId));
    }
}