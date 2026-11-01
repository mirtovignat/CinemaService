package com.example.demo.cursor;

import com.example.demo.dto.base.UserStatus;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.service.linked_collection.UserStatusResolver;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Component
@AllArgsConstructor
public class UserStatusMapperHelper {

    private final UserStatusResolver userStatusResolver;

    public <EntityType extends AbstractCatalogItem, DataType> List<DataType> mapWithStatuses(
            List<EntityType> items,
            BiFunction<EntityType, UserStatus, DataType> mapper) {

        if (items.isEmpty()) {
            return List.of();
        }

        EntityType firstItem = items.get(0);
        Long userId = firstItem.getUser().getId();

        List<Long> movieIds = items.stream()
                .map(item -> item.getMovie().getId())
                .toList();

        Map<Long, UserStatus> userStatuses = userStatusResolver.loadAllUserStatuses(userId, movieIds);

        return items.stream()
                .map(item -> {
                    Long movieId = item.getMovie().getId();
                    UserStatus userStatus = userStatuses.getOrDefault(
                            movieId,
                            UserStatus.builder().build()
                    );
                    return mapper.apply(item, userStatus);
                })
                .toList();
    }
}