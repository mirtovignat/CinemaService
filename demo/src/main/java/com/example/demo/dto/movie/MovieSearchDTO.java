package com.example.demo.dto.movie;

import com.example.demo.dto.base.MovieIdentity;
import com.example.demo.dto.base.MovieMedia;
import com.example.demo.dto.base.MovieRelease;
import com.example.demo.dto.base.MovieTitled;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record MovieSearchDTO(
        @JsonUnwrapped MovieIdentity id,
        @JsonUnwrapped MovieTitled title,
        @JsonUnwrapped MovieRelease release,
        @JsonUnwrapped MovieMedia media,
        String director
) {
}