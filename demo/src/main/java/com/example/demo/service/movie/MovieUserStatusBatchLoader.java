package com.example.demo.service.movie;

import com.example.demo.dto.UserStatus;
import com.example.demo.dto.movie.MovieUserStatus;
import com.example.demo.service.linked_collection.UserStatusResolver;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class MovieUserStatusBatchLoader {

    private final UserStatusResolver userStatusResolver;

    @Transactional(readOnly = true)
    public Map<Long, MovieUserStatus> loadForMovies(List<Long> movieIds, Long userId) {
        if (movieIds == null || movieIds.isEmpty() || userId == null) {
            return Map.of();
        }
        Map<Long, UserStatus> userStatuses = userStatusResolver.loadAllUserStatuses(userId, movieIds);
        Map<Long, MovieUserStatus> movieUserStatuses = new HashMap<>(movieIds.size());

        for (Long movieId : movieIds) {
            UserStatus userStatus = userStatuses.getOrDefault(movieId, UserStatus.EMPTY);
            movieUserStatuses.put(movieId, MovieUserStatus.from(userStatus));
        }
        return movieUserStatuses;
    }
}