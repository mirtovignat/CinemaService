package com.example.demo.service.catalog.manager;

import com.example.demo.dto.response.GroupedHistoryResponse;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.entity.User;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.util.HistoryGrouper;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@AllArgsConstructor
public abstract class AbstractHistoryManager<Entity, DTO> {

    private final UserRepository userRepository;
    private final HistoryGrouper historyGrouper;
    private final UserMapper userMapper;
    private final Function<DTO, LocalDateTime> dateExtractor;

    protected abstract List<Entity> fetchEntities(Long userId);

    protected abstract List<DTO> convertToDtos(List<Entity> entities);

    @Transactional(readOnly = true)
    public GroupedHistoryResponse<DTO> getGroupedHistory(Long userId) {
        User user = userRepository.findByIdOrThrow(userId);
        List<DTO> historyItems = convertToDtos(fetchEntities(userId));
        Map<LocalDate, List<DTO>> groupedItems = historyGrouper.groupByDateDesc(historyItems, dateExtractor);
        UserForOwnerViewDTO userForOwner = userMapper.toOwnerView(user);
        return GroupedHistoryResponse.<DTO>builder()
                .userForOwner(userForOwner)
                .groupedItems(groupedItems)
                .build();
    }
}