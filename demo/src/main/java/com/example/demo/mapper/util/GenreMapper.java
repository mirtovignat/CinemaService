package com.example.demo.mapper.util;

import com.example.demo.model.Genre;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class GenreMapper {

    @Named("genreToString")
    public String toString(Genre genre) {
        return genre == null ? null : genre.getDisplayName();
    }
}