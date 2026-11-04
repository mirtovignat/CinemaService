package com.example.demo.service.movie;

import com.example.demo.repository.entity.RatedMovieRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class MovieReviewCountService {

    private final RatedMovieRepository ratedMovieRepository;

    @Transactional(readOnly = true)
    public long countForMovie(Long movieId) {
        return ratedMovieRepository.countByMovieId(movieId);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> countsByMovieIds(Collection<Long> movieIds) {
        if (movieIds == null || movieIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> reviewCountsByMovieId = new HashMap<>(movieIds.size());

        for (var projection : ratedMovieRepository.countByMovieIds(movieIds)) {
            reviewCountsByMovieId.put(projection.getMovieId(), projection.getTotal());
        }

        for (Long movieId : movieIds) {
            reviewCountsByMovieId.putIfAbsent(movieId, 0L);
        }

        return reviewCountsByMovieId;
    }
}