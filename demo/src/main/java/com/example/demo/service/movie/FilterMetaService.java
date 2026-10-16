package com.example.demo.service.movie;

import com.example.demo.dto.movie.range.RangesSnapshot;
import com.example.demo.mapper.entity.RangesSnapshotMapper;
import com.example.demo.repository.entity.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FilterMetaService {

    private final MovieRepository movieRepository;
    private final RangesSnapshotMapper rangesSnapshotMapper;

    @Cacheable(value = "directors", unless = "#result == null || #result.isEmpty()")
    public List<String> getAllDirectors() {
        return movieRepository.findAllDistinctDirectors();
    }

    @Cacheable(value = "genres", unless = "#result == null || #result.isEmpty()")
    public List<String> getAllGenres() {
        return movieRepository.findAllDistinctGenres();
    }

    public RangesSnapshot getRanges() {
        return rangesSnapshotMapper.toSnapshot(movieRepository.findRanges());
    }
}