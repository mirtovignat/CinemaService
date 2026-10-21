package com.example.demo.cursor;

import com.example.demo.dto.base.UserStatus;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.service.linked_collection.UserStatusResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Component
@RequiredArgsConstructor
public class UserStatusMapperHelper {

    private final UserStatusResolver userStatusResolver;

    public <EntityType, DataType> List<DataType> mapWithStatuses(List<EntityType> items,
                                                                 BiFunction<EntityType, UserStatus, DataType> mapper) {
        if (items.isEmpty()) {
            return List.of();
        }

        AbstractCatalogItem firstItem = (AbstractCatalogItem) items.get(0);
        Long userId = firstItem.getAbstractCatalogItemId().getUserId();

        List<Long> movieIds = items.stream()
                .map(item -> ((AbstractCatalogItem) item).getAbstractCatalogItemId()
                        .getMovieId())
                .toList();

        Map<Long, UserStatus> userStatuses = userStatusResolver.resolveAll(userId, movieIds);

        return items.stream()
                .map(item -> {
                    Long movieId = ((AbstractCatalogItem) item).getAbstractCatalogItemId().getMovieId();
                    UserStatus userStatus = userStatuses.getOrDefault(
                            movieId,
                            UserStatus.builder()
                                    .build()
                    );
                    return mapper.apply(item, userStatus);
                })
                .toList();
    }
}