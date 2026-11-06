package com.example.demo.mapper.util;

import com.example.demo.model.entity.Actor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
public class ActorsMapper {

    @Named("actorsToNames")
    public List<String> actorsToNames(Set<Actor> actors) {
        if (actors == null || actors.isEmpty()) {
            return List.of();
        }
        return actors.stream()
                .map(Actor::getFullName)
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}