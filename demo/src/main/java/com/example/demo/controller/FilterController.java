package com.example.demo.controller;

import com.example.demo.dto.movie.range.Range;
import com.example.demo.service.movie.FilterMetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/filters")
@RequiredArgsConstructor
public class FilterController {

    private final FilterMetaService filterMetaService;

    @GetMapping("/directors")
    public ResponseEntity<List<String>> directors() {
        return ResponseEntity.ok(filterMetaService.getAllDirectors());
    }

    @GetMapping("/genres")
    public ResponseEntity<List<String>> genres() {
        return ResponseEntity.ok(filterMetaService.getAllGenres());
    }

    @GetMapping("/min-max/price")
    public ResponseEntity<Range<BigDecimal>> price() {
        return ResponseEntity.ok(filterMetaService.getRanges().price());
    }

    @GetMapping("/min-max/duration")
    public ResponseEntity<Range<Integer>> duration() {
        return ResponseEntity.ok(filterMetaService.getRanges().duration());
    }

    @GetMapping("/min-max/release-date")
    public ResponseEntity<Range<LocalDate>> releaseDate() {
        return ResponseEntity.ok(filterMetaService.getRanges().releaseDate());
    }
}